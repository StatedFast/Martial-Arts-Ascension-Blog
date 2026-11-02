package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志：每次 HTML 页面命中记一行，是「访问看板」的唯一数据源。
 *
 * <p>刻意不接逻辑删除：日志表只增，历史由定时任务按保留期物理清理。
 * 只落聚合所需的维度（路径 / IP / UA 解析结果 / 来源），不落任何请求体或敏感信息。
 */
@Data
@TableName("blog_visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 访问页面路径，如 /、/article/12、/user/3 */
    private String path;
    /** 页面分类：home / article / user / board / auth / other */
    private String pageType;
    /** 访客 IP（UV 去重口径） */
    private String ip;
    /** 原始 UA（截断存储） */
    private String userAgent;
    /** 解析出的浏览器名 */
    private String browser;
    /** 解析出的操作系统名 */
    private String os;
    /** 设备类型：mobile / desktop / tablet / bot */
    private String deviceType;
    /** 来源页（直接访问 / 站内已归一化） */
    private String referrer;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
