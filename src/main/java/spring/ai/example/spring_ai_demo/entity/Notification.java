package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知。
 *
 * content 是「摘要快照」：写入时把触发者昵称、文章标题拼好存下来，
 * 通知列表页直接返回，不再回表关联 article / comment，避免 N+1。
 */
@Data
@TableName("blog_notification")
public class Notification {

    /** 点赞文章 */
    public static final String TYPE_LIKE = "LIKE";
    /** 收藏文章 */
    public static final String TYPE_FAVORITE = "FAVORITE";
    /** 评论了文章 */
    public static final String TYPE_COMMENT = "COMMENT";
    /** 回复了评论 */
    public static final String TYPE_REPLY = "REPLY";
    /** 被 @ 提及 */
    public static final String TYPE_MENTION = "MENTION";
    /** 系统消息 */
    public static final String TYPE_SYSTEM = "SYSTEM";

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收者 */
    private Long userId;
    /** 触发者，SYSTEM 类型为空 */
    private Long senderId;
    private String type;
    private Long articleId;
    private Long commentId;
    private String content;
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ======== 展示用字段（非数据库列） ========
    @TableField(exist = false)
    private String senderName;
    @TableField(exist = false)
    private String senderAvatar;
}
