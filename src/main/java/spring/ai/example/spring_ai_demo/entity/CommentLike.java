package spring.ai.example.spring_ai_demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞关系。计数同步维护在 blog_comment.like_count 上。
 */
@Data
@TableName("blog_comment_like")
public class CommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
