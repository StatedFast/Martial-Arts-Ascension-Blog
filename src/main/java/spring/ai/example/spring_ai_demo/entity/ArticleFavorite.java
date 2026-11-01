package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章收藏关系。与点赞表同构：唯一索引防重复，取消收藏物理删除。
 */
@Data
@TableName("blog_article_favorite")
public class ArticleFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
