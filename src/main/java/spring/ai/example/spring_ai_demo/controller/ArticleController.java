package spring.ai.example.spring_ai_demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.ArticleDTO;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.service.ArticleService;
import spring.ai.example.spring_ai_demo.service.InteractionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final InteractionService interactionService;

    public ArticleController(ArticleService articleService, InteractionService interactionService) {
        this.articleService = articleService;
        this.interactionService = interactionService;
    }

    /**
     * 文章列表。
     *
     * 标了 @NoAuth 但带 Token 时 JwtInterceptor 仍会解析出 userId，
     * 因此这里能拿到当前用户，用来回显「已赞 / 已收藏」。
     */
    @NoAuth
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "latest") String sort,
                          @RequestAttribute(value = "userId", required = false) Long userId) {
        Page<Article> articlePage = articleService.getArticlePage(page, size, category, keyword, userId, sort);
        return Result.success(articlePage);
    }

    @NoAuth
    @GetMapping("/hot")
    public Result<?> hotArticles(@RequestParam(defaultValue = "5") int limit,
                                 @RequestAttribute(value = "userId", required = false) Long userId) {
        List<Article> hotList = articleService.getHotArticles(limit, userId);
        return Result.success(hotList);
    }

    @NoAuth
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id,
                           @RequestAttribute(value = "userId", required = false) Long userId) {
        Article article = articleService.getArticleDetail(id, userId);
        return Result.success(article);
    }

    @PostMapping
    public Result<?> publish(@RequestAttribute("userId") Long userId,
                             @Valid @RequestBody ArticleDTO dto) {
        Article article = articleService.publish(userId, dto);
        return Result.success(article);
    }

    @PutMapping("/{id}")
    public Result<?> update(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id,
                            @Valid @RequestBody ArticleDTO dto) {
        dto.setId(id);
        Article article = articleService.updateArticle(userId, dto);
        return Result.success(article);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id) {
        articleService.deleteArticle(userId, id);
        return Result.success();
    }

    // ==================== 点赞（可取消、幂等） ====================

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @PostMapping("/{id}/like")
    public Result<?> like(@RequestAttribute("userId") Long userId,
                          @PathVariable Long id) {
        return Result.success(interactionService.likeArticle(userId, id));
    }

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @DeleteMapping("/{id}/like")
    public Result<?> unlike(@RequestAttribute("userId") Long userId,
                            @PathVariable Long id) {
        return Result.success(interactionService.unlikeArticle(userId, id));
    }

    // ==================== 收藏（可取消、幂等） ====================

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @PostMapping("/{id}/favorite")
    public Result<?> favorite(@RequestAttribute("userId") Long userId,
                              @PathVariable Long id) {
        return Result.success(interactionService.favoriteArticle(userId, id));
    }

    @RateLimit(limit = 60, window = 60, message = "操作过于频繁，请稍后再试")
    @DeleteMapping("/{id}/favorite")
    public Result<?> unfavorite(@RequestAttribute("userId") Long userId,
                                @PathVariable Long id) {
        return Result.success(interactionService.unfavoriteArticle(userId, id));
    }

    @GetMapping("/archive")
    public Result<?> archive(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> stats = articleService.getArchiveStats(userId);
        return Result.success(stats);
    }
}
