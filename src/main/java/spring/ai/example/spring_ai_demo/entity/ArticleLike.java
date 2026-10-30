package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 文章点赞关系。
 *
 * 表上有 (article_id, user_id) 唯一索引：
 * - 同一个用户重复点赞只会命中 DuplicateKeyException，服务层捕获后当作成功返回（幂等）
 * - 取消点赞走物理删除，因此本表刻意没有 @TableLogic 字段
 */
@TableName("blog_article_like")
public class ArticleLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
