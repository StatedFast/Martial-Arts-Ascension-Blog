package spring.ai.example.spring_ai_demo.config;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 本地内存缓存服务，替代Redis实现基础缓存功能。
 * 适用于开发环境，无需安装Redis。
 */
@Component
public class LocalCacheService {

    /** 简单KV缓存（支持TTL过期） */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** 计数器 */
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    /** 有序集合（用于热门文章排行） */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> zsets = new ConcurrentHashMap<>();

    /** Set集合 */
    private final ConcurrentHashMap<String, Set<String>> sets = new ConcurrentHashMap<>();

    // ======== 基本KV操作 ========

    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key, entry);
            return null;
        }
        return entry.getValue();
    }

    public void set(String key, Object value) {
        cache.put(key, CacheEntry.permanent(value));
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        cache.put(key, CacheEntry.withTtl(value, unit.toMillis(timeout)));
    }

    public void delete(String key) {
        cache.remove(key);
    }

    public void delete(Collection<String> keys) {
        keys.forEach(cache::remove);
    }

    /**
     * 按前缀批量删除缓存条目，返回删除数量。
     * 供故障演练的「回收已泄漏内存」使用：只清指定前缀（如 article:snapshot:），
     * 不误伤浏览量/限流/热榜等正常缓存。删除后这些条目失去强引用，下次 GC 即可回收。
     */
    public int removeByPrefix(String prefix) {
        int before = cache.size();
        cache.keySet().removeIf(k -> k.startsWith(prefix));
        return before - cache.size();
    }

    // ======== 计数器操作 ========

    public Long increment(String key) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        return counter.incrementAndGet();
    }

    /**
     * 带过期时间的计数器（限流窗口用）。
     *
     * 不用 increment()：counters 里的计数器永不过期，限流 key 会无限堆积且计数只增不减，
     * 最终导致接口被永久封禁。这里把计数放在带 TTL 的 cache 里，窗口到期自动从头计数。
     */
    public Long incrementWithTtl(String key, long timeout, TimeUnit unit) {
        CacheEntry entry = cache.compute(key, (k, old) -> {
            if (old == null || old.isExpired()) {
                return CacheEntry.withTtl(new AtomicLong(1), unit.toMillis(timeout));
            }
            ((AtomicLong) old.getValue()).incrementAndGet();
            return old;
        });
        return ((AtomicLong) entry.getValue()).get();
    }

    // ======== Keys模式匹配 ========

    public Set<String> keys(String pattern) {
        String prefix = pattern.replace("*", "");
        return cache.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && !e.getValue().isExpired())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ======== 有序集合（ZSet）操作 ========

    public void zIncrementScore(String key, String member, double score) {
        ConcurrentHashMap<String, Double> zset = zsets.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        zset.merge(member, score, Double::sum);
    }

    public Set<ZSetEntry> zReverseRangeWithScores(String key, long start, long end) {
        ConcurrentHashMap<String, Double> zset = zsets.get(key);
        if (zset == null || zset.isEmpty()) return Collections.emptySet();

        return zset.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .skip(start)
                .limit(end - start + 1)
                .map(e -> new ZSetEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void zRemoveRange(String key, long start, long end) {
        ConcurrentHashMap<String, Double> zset = zsets.get(key);
        if (zset == null) return;

        List<String> sortedKeys = zset.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (int i = (int) start; i <= (int) end && i < sortedKeys.size(); i++) {
            zset.remove(sortedKeys.get(i));
        }
    }

    // ======== Set操作 ========

    public boolean sIsMember(String key, Object value) {
        Set<String> set = sets.get(key);
        return set != null && set.contains(value.toString());
    }

    // ======== 内部类 ========

    /**
     * 缓存条目：包装缓存值与过期时间
     */
    private static class CacheEntry {
        private final Object value;
        /** 过期时间戳（毫秒），-1表示永不过期 */
        private final long expireAt;

        private CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        static CacheEntry permanent(Object value) {
            return new CacheEntry(value, -1L);
        }

        static CacheEntry withTtl(Object value, long ttlMillis) {
            return new CacheEntry(value, System.currentTimeMillis() + ttlMillis);
        }

        Object getValue() {
            return value;
        }

        boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }

    /**
     * 有序集合条目
     */
    public static class ZSetEntry {
        private final String value;
        private final double score;

        public ZSetEntry(String value, double score) {
            this.value = value;
            this.score = score;
        }

        public String getValue() { return value; }
        public double getScore() { return score; }
    }
}
