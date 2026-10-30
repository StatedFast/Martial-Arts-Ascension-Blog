package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.Comment;

public interface CommentService extends IService<Comment> {

    /** 发表评论（支持楼中楼与 @提及） */
    Comment addComment(Long userId, CommentDTO dto);

    /**
     * 文章评论分页。
     *
     * 一级评论分页，子回复按 parentId 批量装配，避免「一页评论把整篇文章的回复都拉出来」。
     */
    Page<CommentDTO> getArticleComments(Long articleId, int page, int size, Long currentUserId);

    /** 删除评论（作者本人；删除一级评论会连带删除其回复） */
    boolean deleteComment(Long userId, Long commentId);

    /** 获取用户评论数 */
    Long getUserCommentCount(Long userId);

    /** 我的评论（个人内容中心） */
    Page<CommentDTO> getUserComments(Long userId, int page, int size);
}
