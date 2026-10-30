package spring.ai.example.spring_ai_demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI 问答每日配额。
 *
 * 规则：管理员不限次数；普通用户每天最多 {@code ai.daily-limit} 次（默认 10）。
 *
 * 计数按「日期 + 用户」分桶，key 里带当天日期，且 TTL 设为距次日零点的毫秒数：
 * 跨天自然归零，不依赖定时任务；ScheduledTasks 里的清理只是兜底回收历史 key。
 * 计数走 LocalCacheService（内存版），应用重启会清零，属于当前项目的既有取舍。
 */
@Service
public class AiQuotaService {

    /** 配额 key 前缀 */
    public static final String KEY_PREFIX = "ai:daily:";

    /** 管理员角色标识（与 blog_user.role、JWT role claim 一致） */
    public static final String ADMIN_ROLE = "ADMIN";

    private final LocalCacheService cacheService;
    private final int dailyLimit;

    public AiQuotaService(LocalCacheService cacheService,
                          @Value("${ai.daily-limit:10}") int dailyLimit) {
        this.cacheService = cacheService;
        this.dailyLimit = dailyLimit;
    }

    /** 普通用户每日上限（管理员不受限） */
    public int getDailyLimit() {
        return dailyLimit;
    }

    /** 是否管理员：role 为空按普通用户处理 */
    public boolean isAdmin(String role) {
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role.trim());
    }

    /**
     * 消耗一次配额，返回今日累计次数（含本次）。
     * 计数必须带 TTL，否则 key 永久不归零，用户会被永久拒绝。
     */
    public long consume(Long userId) {
        return cacheService.incrementWithTtl(todayKey(userId), millisToMidnight(), TimeUnit.MILLISECONDS);
    }

    /** 今日已用次数 */
    public long used(Long userId) {
        Object value = cacheService.get(todayKey(userId));
        if (value instanceof AtomicLong counter) {
            return counter.get();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    /** 今日剩余次数 */
    public long remaining(Long userId) {
        return Math.max(0L, dailyLimit - used(userId));
    }

    /** 清理非今日的历史配额 key，返回回收数量（定时任务兜底用） */
    public int cleanOutdated() {
        Set<String> keys = cacheService.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        String todayPrefix = KEY_PREFIX + LocalDate.now() + ":";
        Set<String> outdated = keys.stream()
                .filter(key -> !key.startsWith(todayPrefix))
                .collect(Collectors.toSet());
        if (outdated.isEmpty()) {
            return 0;
        }
        cacheService.delete(outdated);
        return outdated.size();
    }

    private String todayKey(Long userId) {
        return KEY_PREFIX + LocalDate.now() + ":" + userId;
    }

    /** 距次日零点的毫秒数，保证当天结束时计数器自动过期 */
    private long millisToMidnight() {
        long millis = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toMillis();
        return Math.max(1000L, millis);
    }
}
