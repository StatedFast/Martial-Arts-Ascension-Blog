package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 系统配置项（k-v），供后台「配置中心」读写。
 *
 * 用途之一是做故障演练开关：定时任务轮询这里的开关，按需注入内存泄漏等故障，
 * 用于在无真实流量的开发环境复现、排查生产级问题（如 OOM）。
 */
@TableName("blog_sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置键，如 fault.oom.enabled */
    private String configKey;
    /** 配置值，统一以字符串存储，读取时按目标类型解析 */
    private String configValue;
    /** 中文说明，前端配置中心直接展示 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ======== Getters & Setters ========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
