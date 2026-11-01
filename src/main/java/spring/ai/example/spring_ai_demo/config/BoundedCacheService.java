package spring.ai.example.spring_ai_demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 有界 LRU 缓存 —— LocalCacheService 无限增长问题的「修复版」演示。
 *
 * 对照泄漏点：LocalCacheService.cache 是裸 ConcurrentHashMap，写入只进不出、无容量上限、无淘汰，
 * 一旦 key 设计错误（如带时间戳）就会稳定涨到堆满，最终 Java heap space OOM。
 *
 * 修复三要素（面试标准答案）：
 * 1. 容量上限：maximumSize，超出即淘汰，堆占用有天花板；
 * 2. 淘汰策略：LRU（accessOrder=true，命中即提到队尾，淘汰最久未访问的）；
 * 3. 过期兜底：条目可带 TTL，读取时惰性淘汰（这里保留简化实现，够演示）。
 *
 * 生产更推荐直接用 Caffeine（expireAfterWrite + maximumSize），本类只为「看得见原理」的教学对比。
 */
@Component
public class BoundedCacheService {

    private static final Logger log = LoggerFactory.getLogger(BoundedCacheService.class);

    /** 最大条目数（默认 32；配合 1MB 负载，堆占用天花板约 32MB，256MB 小堆下曲线一眼走平） */
    private final int maxSize;

    /** accessOrder=true：每次访问把条目移到链表尾部，队首即最久未用，超限时淘汰队首 */
    private final LinkedHashMap<String, byte[]> map;

    /** 累计因超限被淘汰的条目数，供演示统计「淘汰发生了多少次」 */
    private volatile long evictedCount = 0;

    public BoundedCacheService(@Value("${fault.drill.bounded-cache-max-size:32}") int maxSize) {
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                boolean shouldRemove = size() > BoundedCacheService.this.maxSize;
                if (shouldRemove) {
                    evictedCount++;
                }
                return shouldRemove;
            }
        };
        log.info("有界 LRU 缓存初始化，容量上限={} 条", maxSize);
    }

    /**
     * 写入：超出容量时 LinkedHashMap 自动淘汰最久未访问的条目，堆占用被钳制在 maxSize 内。
     */
    public synchronized void put(String key, byte[] value) {
        map.put(key, value);
    }

    public synchronized byte[] get(String key) {
        return map.get(key);
    }

    public synchronized int size() {
        return map.size();
    }

    public long evictedCount() {
        return evictedCount;
    }

    /** 当前缓存实际承载的键（快照），供配置中心展示 LRU 是否真的只保留最近 N 个 */
    public synchronized List<String> keysSnapshot() {
        return new ArrayList<>(map.keySet());
    }
}
