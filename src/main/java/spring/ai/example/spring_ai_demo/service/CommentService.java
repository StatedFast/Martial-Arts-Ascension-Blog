package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.Comment;

import java.util.List;

public interface CommentService extends IService<Comment> {

    /** 发表评论 */
    Comment addComment(Long userId, CommentDTO dto);

    /** 获取文章评论列表（树形结构） */
    List<CommentDTO> getArticleComments(Long articleId);

    /** 删除评论 */
    boolean deleteComment(Long userId, Long commentId);

    /** 获取用户评论数 */
    Long getUserCommentCount(Long userId);
}
