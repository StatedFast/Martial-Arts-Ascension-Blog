package spring.ai.example.spring_ai_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流服务（防暴力破解）
 *
 * 企业级特性：
 * - 同一账号连续失败N次后锁定（默认5次）
 * - 锁定时间窗口（默认30分钟）
 * - 同一IP失败次数限制（防分布式暴力破解）
 * - 自动解锁（过期清除）
 *
 * 生产环境建议替换为Redis实现（支持分布式部署）。
 */
@Service
@Slf4j
public class LoginAttemptService {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${security.login.ip-max-attempts:20}")
    private int ipMaxAttempts;

    /**
     * 账号失败记录
     * key: username, value: LoginAttempt
     */
    private final ConcurrentHashMap<String, LoginAttempt> accountAttempts = new ConcurrentHashMap<>();

    /**
     * IP失败记录
     * key: ip, value: LoginAttempt
     */
    private final ConcurrentHashMap<String, LoginAttempt> ipAttempts = new ConcurrentHashMap<>();

    /**
     * 检查账号是否被锁定
     */
    public boolean isAccountLocked(String username) {
        LoginAttempt attempt = accountAttempts.get(username.toLowerCase());
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired()) {
            accountAttempts.remove(username.toLowerCase());
            return false;
        }
        return attempt.count >= maxAttempts;
    }

    /**
     * 检查IP是否被限制
     */
    public boolean isIpBlocked(String ip) {
        LoginAttempt attempt = ipAttempts.get(ip);
        if (attempt == null) {
            return false;
        }
        if (attempt.isExpired()) {
            ipAttempts.remove(ip);
            return false;
        }
        return attempt.count >= ipMaxAttempts;
    }

    /**
     * 记录登录失败
     */
    public void loginFailed(String username, String ip) {
        // 记录账号失败
        accountAttempts.compute(username.toLowerCase(), (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LoginAttempt(1, System.currentTimeMillis() + (long) lockDurationMinutes * 60 * 1000);
            }
            existing.count++;
            return existing;
        });

        // 记录IP失败
        ipAttempts.compute(ip, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LoginAttempt(1, System.currentTimeMillis() + (long) lockDurationMinutes * 60 * 1000);
            }
            existing.count++;
            return existing;
        });

        LoginAttempt attempt = accountAttempts.get(username.toLowerCase());
        int remaining = maxAttempts - (attempt != null ? attempt.count : 0);
        if (remaining <= 2 && remaining > 0) {
            log.warn("登录失败警告: username={}, ip={}, 剩余尝试次数={}", username, ip, remaining);
        }
        if (remaining <= 0) {
            log.warn("账号已锁定: username={}, ip={}, 锁定{}分钟", username, ip, lockDurationMinutes);
        }
    }

    /**
     * 登录成功，清除失败记录
     */
    public void loginSucceeded(String username, String ip) {
        accountAttempts.remove(username.toLowerCase());
        // IP记录不立即清除（防止同一IP下多个账号被暴力尝试）
        log.debug("登录成功，清除失败记录: username={}", username);
    }

    /**
     * 获取账号剩余尝试次数
     */
    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = accountAttempts.get(username.toLowerCase());
        if (attempt == null || attempt.isExpired()) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - attempt.count);
    }

    /**
     * 定时清理过期记录（每15分钟）
     */
    @Scheduled(fixedRate = 900000)
    public void cleanExpiredEntries() {
        int beforeAccount = accountAttempts.size();
        int beforeIp = ipAttempts.size();

        accountAttempts.entrySet().removeIf(e -> e.getValue().isExpired());
        ipAttempts.entrySet().removeIf(e -> e.getValue().isExpired());

        int removedAccount = beforeAccount - accountAttempts.size();
        int removedIp = beforeIp - ipAttempts.size();
        if (removedAccount > 0 || removedIp > 0) {
            log.debug("登录限流清理: 移除{}条账号记录, {}条IP记录", removedAccount, removedIp);
        }
    }

    /**
     * 登录尝试记录
     */
    private static class LoginAttempt {
        int count;
        final long expireAt;

        LoginAttempt(int count, long expireAt) {
            this.count = count;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
