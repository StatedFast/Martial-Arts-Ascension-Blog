package spring.ai.example.spring_ai_demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.config.BoundedCacheService;
import spring.ai.example.spring_ai_demo.config.LeakSimulator;

import java.io.File;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM 运行时指标采集（看板与配置中心共用同一份口径）。
 *
 * <p>抽出来的原因：「渡劫看板」要对所有人开放、「配置中心」只给管理员，
 * 两边看到的是同一组数字，绝不能各算各的 —— 否则同一时刻两个页面百分比不一致，
 * 演练时根本没法判断到底是谁的口径错了。
 *
 * <p>无接口独立 @Service：与 LeakSimulator 同理，避免 @Scheduled/JMX 逻辑落到
 * JDK 动态代理的接口 Bean 上。
 */
@Service
@RequiredArgsConstructor
public class MonitorMetricsService {

    private final SysConfigService sysConfigService;
    private final BoundedCacheService boundedCacheService;
    private final LeakSimulator leakSimulator;

    /**
     * 实时内存快照（MB）+ 演练状态。看板据此画内存曲线。
     */
    public Map<String, Object> memorySnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long heapMaxMb = runtime.maxMemory() / (1024 * 1024);

        long nonHeapUsedMb = ManagementFactory.getMemoryMXBean()
                .getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        // 堆外直接内存：MemoryMXBean 的 nonHeap 照不到它，必须走 BufferPoolMXBean("direct") 口径
        long directUsedMb = 0;
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if ("direct".equals(pool.getName())) {
                directUsedMb = pool.getMemoryUsed() / (1024 * 1024);
            }
        }

        // CPU：com.sun.management 扩展口径（HotSpot 自带），取「进程」CPU 占用率；首个采样窗口返回 -1，按 0 处理
        long cpuUsagePercent = 0;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            double load = os.getProcessCpuLoad();
            if (load >= 0) {
                cpuUsagePercent = Math.round(load * 100);
            }
        }

        // 磁盘：取应用运行所在分区（user.dir 对应盘），(总-可用)/总
        File store = new File(System.getProperty("user.dir"));
        long diskTotal = store.getTotalSpace();
        long diskUsagePercent = diskTotal <= 0 ? 0
                : Math.round((diskTotal - store.getUsableSpace()) * 100.0 / diskTotal);

        // GC 累计次数与耗时：OOM 前真正的信号是「GC 越跑越勤、单次越来越长」，比单看堆占用率更早暴露问题
        long gcCount = 0;
        long gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() >= 0) {
                gcCount += gc.getCollectionCount();
            }
            if (gc.getCollectionTime() >= 0) {
                gcTimeMs += gc.getCollectionTime();
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("heapUsedMb", heapUsedMb);
        data.put("heapMaxMb", heapMaxMb);
        data.put("nonHeapUsedMb", nonHeapUsedMb);
        data.put("directUsedMb", directUsedMb);
        data.put("directMaxMb", resolveMaxDirectMb());
        data.put("cpuUsagePercent", cpuUsagePercent);
        data.put("diskUsagePercent", diskUsagePercent);
        data.put("gcCount", gcCount);
        data.put("gcTimeMs", gcTimeMs);
        // 泄漏存量三视角：累计注入（流水）/ 堆内条数 / 堆外 Buffer 个数（存量），区分「泄了多少」与「还攒着多少」
        data.put("injectedMbTotal", leakSimulator.getInjectedMb());
        data.put("leakEntries", leakSimulator.leakEntryCount());
        data.put("directBuffers", leakSimulator.directBufferCount());
        data.put("batchSize", sysConfigService.getClampedBatchSize());
        data.put("faultEnabled", sysConfigService.getBoolean(SysConfigService.FAULT_ENABLED));
        data.put("faultType", sysConfigService.getFaultType());
        data.put("faultFixed", sysConfigService.getBoolean(SysConfigService.FAULT_FIXED));
        // 修复模式生效时，展示有界 LRU 当前条目数与累计淘汰次数，直观证明「淘汰在工作、内存封顶」
        data.put("boundedCacheSize", boundedCacheService.size());
        data.put("boundedCacheEvicted", boundedCacheService.evictedCount());
        // 刷新间隔随响应下发：配置中心改完后，前端下一轮自动按新间隔重排定时器
        data.put("pollIntervalSeconds", sysConfigService.getPollIntervalSeconds());
        return data;
    }

    /**
     * JVM 核心参数（只读）。演练时的「边界常量速查」：-Xmx / MaxDirectMemorySize / GC 就是各种 OOM 会撞的墙。
     * 全部来自运行时真实值而非 blog_sys_config 表，前端渲染成不可编辑的展示行。
     *
     * @param withEnvFingerprint 是否附带 JDK 版本与操作系统信息。
     *                           看板对游客开放，这类环境指纹属于「无收益的泄露」（攻击者可用它比对 CVE），
     *                           所以只在管理员的配置中心里给全量。
     */
    public List<Map<String, String>> jvmInfo(boolean withEnvFingerprint) {
        Runtime rt = Runtime.getRuntime();
        String xms = findVmArg("-Xms");
        String xmx = findVmArg("-Xmx");
        StringBuilder gcNames = new StringBuilder();
        ManagementFactory.getGarbageCollectorMXBeans()
                .forEach(bean -> gcNames.append(bean.getName()).append("  "));
        String allXArgs = String.join("  ", ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(a -> a.startsWith("-X")).toList());

        List<Map<String, String>> info = new ArrayList<>();
        info.add(roRow("-Xms", "堆初始化大小（未设则为动态值）", xms == null ? "未设置" : xms));
        info.add(roRow("-Xmx", "堆最大 —— heap 演练撞的墙", xmx == null
                ? rt.maxMemory() / (1024 * 1024) + " MB" : xmx));
        info.add(roRow("-XX:MaxDirectMemorySize", "直接内存上限 —— direct 演练撞的墙（未显式设置时默认≈Xmx）",
                resolveMaxDirectMb() + " MB"));
        info.add(roRow("GarbageCollector", "垃圾收集器（GC 日志/曲线解读的基础）", gcNames.toString().trim()));
        info.add(roRow("availableProcessors", "JVM 可见 CPU 核数", String.valueOf(rt.availableProcessors())));
        if (withEnvFingerprint) {
            info.add(roRow("java.version", "JDK 版本", System.getProperty("java.version")
                    + " (" + System.getProperty("java.vm.name") + ", " + System.getProperty("java.vendor") + ")"));
            info.add(roRow("os.name / os.arch", "运行操作系统",
                    System.getProperty("os.name") + " " + System.getProperty("os.arch")));
        }
        info.add(roRow("启动 -X 参数全集", "当前进程实际生效的全部内存/GC 参数",
                allXArgs.isEmpty() ? "（无 -X 参数，全部默认）" : allXArgs));
        return info;
    }

    /** JVM 只读参数行：{key, desc, value}，与前端表格列一一对应 */
    private Map<String, String> roRow(String key, String desc, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("desc", desc);
        row.put("value", value);
        return row;
    }

    /** 在 JVM 启动参数里找指定前缀的参数（如 -Xms256m），找不到返回 null */
    private String findVmArg(String prefix) {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith(prefix)) {
                return arg;
            }
        }
        return null;
    }

    /**
     * 解析直接内存上限（MB）：MaxDirectMemorySize 不在 JMX 标准接口里，只能从启动参数读；
     * 未显式设置时 JVM 默认≈最大堆，这本身就是堆外演练要展示的知识点（run.bat 里显式设了 64m）。
     */
    private long resolveMaxDirectMb() {
        long heapFallback = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-XX:MaxDirectMemorySize=")) {
                continue;
            }
            String v = arg.substring("-XX:MaxDirectMemorySize=".length()).toUpperCase().trim();
            try {
                long num = Long.parseLong(v.replaceAll("[KMG]$", ""));
                if (v.endsWith("G")) {
                    return num * 1024;
                }
                if (v.endsWith("K")) {
                    return num / 1024;
                }
                return num; // 默认 M（纯数字按字节的情况极少，演练参数固定带 M）
            } catch (NumberFormatException e) {
                return heapFallback;
            }
        }
        return heapFallback;
    }
}
