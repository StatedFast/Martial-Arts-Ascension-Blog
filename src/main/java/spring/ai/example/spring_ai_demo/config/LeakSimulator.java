package spring.ai.example.spring_ai_demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.service.SysConfigService;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 故障演练注入器（Fault / Chaos Injector）。
 *
 * 由配置中心的开关驱动，定时轮询 blog_sys_config：开关打开时按节奏注入内存故障，
 * 用于在无真实流量的开发环境复现生产级 OOM，配合 -Xmx / -XX:MaxDirectMemorySize
 * 与 -XX:+HeapDumpOnOutOfMemoryError 观察、排查、复盘。开关关闭即完全停止，零副作用。
 *
 * 两种故障形态（fault.oom.type）：
 * - heap  ：堆内泄漏。往 LocalCacheService 写入「时间戳 key + 永不过期 + 大对象」的缓存条目，
 *          模拟一次错误的「缓存优化」——只进不出，old gen 阶梯上涨直至 Java heap space OOM。
 * - direct：堆外泄漏。持续 allocateDirect 且用静态 List 长期强引用，使 Cleaner 无法回收，
 *          复现 Direct buffer memory OOM（不受 -Xmx 约束，需要 -XX:MaxDirectMemorySize 才收敛）。
 *
 * 做成无接口独立组件的原因同 NotificationSseManager：@Scheduled 不能落在被 JDK 动态代理的接口 Bean 上。
 */
@Component
public class LeakSimulator {

    private static final Logger log = LoggerFactory.getLogger(LeakSimulator.class);

    private final LocalCacheService cacheService;
    private final SysConfigService sysConfigService;
    private final BoundedCacheService boundedCacheService;

    /** 堆外泄漏载体：必须长期强引用 DirectByteBuffer，否则无引用会被 Cleaner 及时回收而泄不出来 */
    private final List<ByteBuffer> directBufferPool = new CopyOnWriteArrayList<>();

    /** 修复模式下堆外「有界引用池」的上限：超出即丢弃最老引用，让 Cleaner 能回收，堆外占用封顶 */
    private static final int FIXED_DIRECT_POOL_SIZE = 32;

    /** 累计注入量（MB），仅用于日志观测 */
    private volatile long injectedMb = 0;

    public LeakSimulator(LocalCacheService cacheService, SysConfigService sysConfigService,
                         BoundedCacheService boundedCacheService) {
        this.cacheService = cacheService;
        this.sysConfigService = sysConfigService;
        this.boundedCacheService = boundedCacheService;
    }

    /**
     * 每 5 秒轮询一次开关。未开启直接 return，不做任何分配。
     * fixedDelay 而非 fixedRate：确保上一轮注入完成后再计时，避免泄漏任务自身堆积。
     */
    @Scheduled(fixedDelay = 5000)
    public void tick() {
        if (!sysConfigService.getBoolean(SysConfigService.FAULT_ENABLED)) {
            return;
        }

        int batch = sysConfigService.getClampedBatchSize();
        String type = sysConfigService.getFaultType();
        boolean fixed = sysConfigService.getBoolean(SysConfigService.FAULT_FIXED);
        try {
            if ("direct".equals(type)) {
                injectDirect(batch, fixed);
            } else {
                injectHeap(batch, fixed);
            }
            injectedMb += batch;
            log.warn("【故障演练】type={} fixed={} 本轮注入 {}MB，累计 {}MB（开关关闭即停止）",
                    type, fixed, batch, injectedMb);
        } catch (OutOfMemoryError oom) {
            // 让 JVM 按 -XX:+HeapDumpOnOutOfMemoryError 自行 dump 并终止，这里只做一行清晰日志
            log.error("【故障演练】注入触发 OOM，预期结果，请查看 heap dump 分析: {}", oom.getMessage());
            throw oom;
        }
    }

    /**
     * 堆内注入：
     * - 泄漏态（fixed=false）：写 LocalCacheService，时间戳 key + 永不过期 + 无淘汰 → old gen 稳定上涨至 OOM；
     * - 修复态（fixed=true）：同样负载写有界 LRU 缓存，超容量自动淘汰 → 堆占用封顶，曲线走平。
     *   这就是面试演示的「同一段代码，换对的缓存实现，前后内存曲线对比闭环」。
     */
    private void injectHeap(int batch, boolean fixed) {
        for (int i = 0; i < batch; i++) {
            byte[] payload = new byte[SysConfigService.CHUNK_BYTES];
            if (fixed) {
                // 修复：交给有界 LRU，超过 maxSize 由 removeEldestEntry 淘汰最久未用条目
                boundedCacheService.put("article:snapshot:" + System.nanoTime(), payload);
            } else {
                // 泄漏：故意用永久 set（非带 TTL 重载），key 唯一，模拟错误的缓存优化
                cacheService.set("article:snapshot:" + System.nanoTime(), payload);
            }
        }
    }

    /**
     * 堆外注入：
     * - 泄漏态（fixed=false）：DirectByteBuffer 无限攒进引用池，撑爆 -XX:MaxDirectMemorySize；
     * - 修复态（fixed=true）：引用池封顶 FIXED_DIRECT_POOL_SIZE，丢弃最老引用使 Cleaner 可回收，堆外占用封顶。
     */
    private void injectDirect(int batch, boolean fixed) {
        for (int i = 0; i < batch; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(SysConfigService.CHUNK_BYTES);
            buffer.put((byte) 1); // 触碰一下确保真正占用
            directBufferPool.add(buffer);
            if (fixed && directBufferPool.size() > FIXED_DIRECT_POOL_SIZE) {
                // 丢弃最老引用（下标 0），失去强引用后由 Cleaner 回收其直接内存
                directBufferPool.remove(0);
            }
        }
    }

    /**
     * 应急止血：管理员可在配置中心关开关后，调用此逻辑主动释放已注入的直接内存引用，
     * 让 Cleaner 回收，避免必须重启。目前仅暴露方法，暂不接前端（保持演练的“必须重启恢复”真实感）。
     */
    public void releaseDirectBuffers() {
        int size = directBufferPool.size();
        directBufferPool.clear();
        log.warn("【故障演练】已释放 {} 个 DirectByteBuffer 强引用，等待 Cleaner 回收", size);
    }
}
