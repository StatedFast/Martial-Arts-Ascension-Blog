package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 文章收藏关系。与点赞表同构：唯一索引防重复，取消收藏物理删除。
 */
@TableName("blog_article_favorite")
public class ArticleFavorite {

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
