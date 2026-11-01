package spring.ai.example.spring_ai_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.SysConfig;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.mapper.SysConfigMapper;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;
import spring.ai.example.spring_ai_demo.service.SysConfigService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据初始化器（企业级）
 *
 * 功能：
 * - 应用启动时自动检测并迁移旧版MD5密码为BCrypt格式
 * - 确保种子数据（data.sql）中的用户能正常登录
 * - 仅执行一次迁移，后续启动自动跳过
 *
 * 生产环境中，密码迁移通常通过数据迁移脚本完成。
 * 此处为开发/演示环境的自动迁移方案。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /** 种子用户的默认密码（仅开发环境，与 data.sql 中 MD5 对应的原始密码一致） */
    private static final String DEFAULT_SEED_PASSWORD = "123456";

    @Override
    public void run(String... args) {
        migratePasswordsToBCrypt();
        seedSysConfig();
    }

    /**
     * 播种配置中心默认项（幂等：已存在的 key 不覆盖，保留管理员手动改过的值）。
     * 故障演练开关默认全部关闭，batch-size 给一个温和值，需管理员显式打开才生效。
     */
    private void seedSysConfig() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(SysConfigService.FAULT_ENABLED, "false|故障演练总开关（true 开启内存泄漏注入，关闭即止血）");
        defaults.put(SysConfigService.FAULT_TYPE, "heap|故障类型：heap=堆内泄漏 / direct=堆外直接内存泄漏");
        defaults.put(SysConfigService.FAULT_BATCH_SIZE, "20|每5秒注入的MB数（后端钳制上限50，防止爆得太快看不到GC过程）");
        defaults.put(SysConfigService.FAULT_FIXED, "false|修复模式（true=注入走有界LRU/有界引用池，内存曲线走平；与泄漏态对比演示修复效果）");
        defaults.put(SysConfigService.MONITOR_POLL_INTERVAL, "3|配置中心内存曲线刷新间隔（秒，1-60，保存后前端自动生效）");

        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            Long exists = sysConfigMapper.selectCount(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, entry.getKey()));
            if (exists != null && exists > 0) {
                continue;
            }
            String[] parts = entry.getValue().split("\\|", 2);
            SysConfig config = new SysConfig();
            config.setConfigKey(entry.getKey());
            config.setConfigValue(parts[0]);
            config.setDescription(parts.length > 1 ? parts[1] : null);
            sysConfigMapper.insert(config);
        }
        log.debug("配置中心默认项播种完成");
    }

    /**
     * 检测并迁移非BCrypt格式的密码
     * BCrypt哈希以 $2a$ 或 $2b$ 开头，如果不是则说明是旧格式
     */
    private void migratePasswordsToBCrypt() {
        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>());

        int migratedCount = 0;
        for (User user : allUsers) {
            if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
                // 非BCrypt格式，执行迁移
                user.setPassword(passwordEncoder.encode(DEFAULT_SEED_PASSWORD));
                userMapper.updateById(user);
                migratedCount++;
            }
        }

        if (migratedCount > 0) {
            log.info("密码迁移完成: {}个用户已从MD5迁移到BCrypt，默认密码: {}", migratedCount, DEFAULT_SEED_PASSWORD);
        } else {
            log.debug("密码格式检查完成，无需迁移");
        }
    }
}
