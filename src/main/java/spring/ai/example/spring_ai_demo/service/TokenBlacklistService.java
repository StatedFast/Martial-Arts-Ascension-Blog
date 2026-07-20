package spring.ai.example.spring_ai_demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token黑名单服务
 *
 * 用于实现Token注销/吊销功能。
 * 当用户登出或修改密码时，将Token的jti加入黑名单，
 * 后续请求携带该Token将被拒绝。
 *
 * 生产环境应替换为Redis实现（利用Redis TTL自动过期）：
 *   redisTemplate.opsForValue().set("jwt:blacklist:" + jti, "1", remainingMillis, TimeUnit.MILLISECONDS);
 *
 * 当前使用本地内存实现，适用于开发/演示环境。
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final LocalCacheService cacheService;

    /**
     * 存储黑名单条目及其过期时间戳
     * key: jti, value: 过期时间戳(ms)
     */
    private final ConcurrentHashMap<String, Long> blacklistExpiry = new ConcurrentHashMap<>();

    public TokenBlacklistService(LocalCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 将Token加入黑名单
     *
     * @param jti            Token的唯一标识
     * @param remainingMillis Token剩余有效时间（毫秒），黑名单只保留到Token自然过期
     */
    public void addToBlacklist(String jti, long remainingMillis) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        long expiryTimestamp = System.currentTimeMillis() + Math.max(remainingMillis, 0);
        blacklistExpiry.put(jti, expiryTimestamp);
        cacheService.set(BLACKLIST_PREFIX + jti, expiryTimestamp);
        log.info("Token已加入黑名单: jti={}, 将在{}ms后自动清除", jti, remainingMillis);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param jti Token的唯一标识
     * @return true=已被吊销
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }

        Long expiryTimestamp = blacklistExpiry.get(jti);
        if (expiryTimestamp == null) {
            return false;
        }

        // 如果已过Token自然过期时间，从黑名单中移除（Token本身已无效）
        if (System.currentTimeMillis() > expiryTimestamp) {
            blacklistExpiry.remove(jti);
            cacheService.delete(BLACKLIST_PREFIX + jti);
            return false;
        }

        return true;
    }

    /**
     * 获取当前黑名单中的Token数量
     */
    public int getBlacklistSize() {
        return blacklistExpiry.size();
    }

    /**
     * 定时清理过期的黑名单条目（每10分钟执行一次）
     * 生产环境使用Redis TTL自动过期，无需此定时任务
     */
    @Scheduled(fixedRate = 600000)
    public void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        int before = blacklistExpiry.size();

        blacklistExpiry.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                cacheService.delete(BLACKLIST_PREFIX + entry.getKey());
                return true;
            }
            return false;
        });

        int removed = before - blacklistExpiry.size();
        if (removed > 0) {
            log.debug("黑名单清理完成，移除了{}条过期条目，剩余{}条", removed, blacklistExpiry.size());
        }
    }
}
