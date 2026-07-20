package spring.ai.example.spring_ai_demo.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;

import java.util.List;

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
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /** 种子用户的默认密码（仅开发环境） */
    private static final String DEFAULT_SEED_PASSWORD = "Abc@1234";

    public DataInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        migratePasswordsToBCrypt();
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
