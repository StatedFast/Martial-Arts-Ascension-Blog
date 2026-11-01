package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.ai.example.spring_ai_demo.entity.SysConfig;
import spring.ai.example.spring_ai_demo.mapper.SysConfigMapper;

import java.util.List;

/**
 * 系统配置读写服务（配置中心 + 故障演练开关）。
 *
 * 设计约束：
 * - 做成无接口的独立 @Service，与 NotificationSseManager 同理，
 *   避免被 JDK 动态代理后 @Scheduled / 内部方法在代理上找不到的启动失败问题。
 * - 读取开关一律直连数据库，绝不经过 LocalCacheService。
 *   原因：故障演练本身会污染 LocalCacheService（内存泄漏就发生在那个缓存里），
 *   若开关也存进去，会出现「泄漏缓存把开关挤掉 / 自己依赖自己」的死锁式故障，
 *   导致演练无法被关掉。开关必须独立于被注入故障的存储。
 */
@Service
public class SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigService.class);

    private final SysConfigMapper sysConfigMapper;

    public SysConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    // ======== 配置键常量（故障演练） ========

    /** 故障演练总开关：true/false */
    public static final String FAULT_ENABLED = "fault.oom.enabled";
    /** 故障类型：heap（堆内泄漏）/ direct（堆外直接内存泄漏） */
    public static final String FAULT_TYPE = "fault.oom.type";
    /** 每轮注入的条数 */
    public static final String FAULT_BATCH_SIZE = "fault.oom.batch-size";
    /** 每条负载大小（MB），仅用于展示/换算，实际注入以条数为准 */
    public static final String FAULT_PAYLOAD_MB = "fault.oom.payload-mb";
    /** 修复模式：true 时注入走「有界缓存 / 有界引用池」，内存封顶曲线走平（用于修复前后对比演示） */
    public static final String FAULT_FIXED = "fault.oom.fixed";
    /** 配置中心内存曲线刷新间隔（秒），前端每轮读取、变更后自动重排定时器 */
    public static final String MONITOR_POLL_INTERVAL = "monitor.poll-interval-seconds";

    /** 每条固定 1MB，batch 直接约等于 MB 数，便于按「泄多少内存」直观调参 */
    public static final int CHUNK_BYTES = 1024 * 1024;
    /** batch-size 后端钳制上限，防手滑几十秒就爆、观察不到 GC 过程 */
    private static final int MAX_BATCH_SIZE = 50;

    // ======== 读取 ========

    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }

    public String getString(String key, String defaultValue) {
        SysConfig config = selectByKey(key);
        return config == null || config.getConfigValue() == null
                ? defaultValue : config.getConfigValue();
    }

    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key, "false").trim());
    }

    /**
     * 读取整数配置并钳制到 [1, MAX_BATCH_SIZE]。
     * 解析失败按 1 处理，保证注入节奏可控。
     */
    public int getClampedBatchSize() {
        int size;
        try {
            size = Integer.parseInt(getString(FAULT_BATCH_SIZE, "20").trim());
        } catch (NumberFormatException e) {
            size = 20;
        }
        return Math.max(1, Math.min(size, MAX_BATCH_SIZE));
    }

    public String getFaultType() {
        return getString(FAULT_TYPE, "heap").trim().toLowerCase();
    }

    /** 刷新间隔（秒），解析失败给默认 3，钳制到 [1, 60] 防手滑填 0 把前端轮询打死 */
    public int getPollIntervalSeconds() {
        int seconds;
        try {
            seconds = Integer.parseInt(getString(MONITOR_POLL_INTERVAL, "3").trim());
        } catch (NumberFormatException e) {
            seconds = 3;
        }
        return Math.max(1, Math.min(seconds, 60));
    }

    // ======== 写入 ========

    /**
     * 更新（或创建）配置值，返回受影响行数说明。仅配置中心管理员调用。
     */
    public boolean updateValue(String key, String value) {
        SysConfig config = selectByKey(key);
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            int rows = sysConfigMapper.insert(config);
            log.info("新建配置项: {}={}", key, value);
            return rows > 0;
        }
        config.setConfigValue(value);
        int rows = sysConfigMapper.updateById(config);
        log.info("更新配置项: {}={}", key, value);
        return rows > 0;
    }

    private SysConfig selectByKey(String key) {
        return sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
    }
}
