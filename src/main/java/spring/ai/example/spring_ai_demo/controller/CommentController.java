package spring.ai.example.spring_ai_demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.Comment;
import spring.ai.example.spring_ai_demo.service.CommentService;
import spring.ai.example.spring_ai_demo.service.InteractionService;

@RestController
@RequestMapping("/api/articles")
public class CommentController {

    private final CommentService commentService;
    private final InteractionService interactionService;

    public CommentController(CommentService commentService, InteractionService interactionService) {
        this.commentService = commentService;
        this.interactionService = interactionService;
    }

    /**
     * 评论分页：一级评论分页返回，子回复挂在 children 上一起返回。
     * 返回 Page，前端用 total/pages 渲染分页，不再一次性拉全量评论。
     */
    @NoAuth
    @GetMapping("/{articleId}/comments")
    public Result<?> list(@PathVariable Long articleId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestAttribute(value = "userId", required = false) Long userId) {
        Page<CommentDTO> comments = commentService.getArticleComments(articleId, page, size, userId);
        return Result.success(comments);
    }

    @RateLimit(limit = 20, window = 60, message = "评论太频繁，请休息一下")
    @PostMapping("/{articleId}/comments")
    public Result<?> addComment(@RequestAttribute("userId") Long userId,
                                @PathVariable Long articleId,
                                @Valid @RequestBody CommentDTO dto) {
        dto.setArticleId(articleId);
        Comment comment = commentService.addComment(userId, dto);
        return Result.success(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<?> deleteComment(@RequestAttribute("userId") Long userId,
                                   @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
        return Result.success();
    }

    // ==================== 评论点赞 ====================

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @PostMapping("/comments/{commentId}/like")
    public Result<?> likeComment(@RequestAttribute("userId") Long userId,
                                 @PathVariable Long commentId) {
        return Result.success(interactionService.likeComment(userId, commentId));
    }

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @DeleteMapping("/comments/{commentId}/like")
    public Result<?> unlikeComment(@RequestAttribute("userId") Long userId,
                                   @PathVariable Long commentId) {
        return Result.success(interactionService.unlikeComment(userId, commentId));
    }
}
