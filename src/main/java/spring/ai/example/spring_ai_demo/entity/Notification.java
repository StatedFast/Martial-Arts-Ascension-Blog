package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 站内通知。
 *
 * content 是「摘要快照」：写入时把触发者昵称、文章标题拼好存下来，
 * 通知列表页直接返回，不再回表关联 article / comment，避免 N+1。
 */
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
}
