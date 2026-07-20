package spring.ai.example.spring_ai_demo.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.Comment;
import spring.ai.example.spring_ai_demo.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @NoAuth
    @GetMapping("/{articleId}/comments")
    public Result<?> list(@PathVariable Long articleId) {
        List<CommentDTO> comments = commentService.getArticleComments(articleId);
        return Result.success(comments);
    }

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
}
