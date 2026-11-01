package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章点赞关系。
 *
 * 表上有 (article_id, user_id) 唯一索引：
 * - 同一个用户重复点赞只会命中 DuplicateKeyException，服务层捕获后当作成功返回（幂等）
 * - 取消点赞走物理删除，因此本表刻意没有 @TableLogic 字段
 */
@Data
@TableName("blog_article_like")
public class ArticleLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
