package spring.ai.example.spring_ai_demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.dto.ArticleDTO;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.service.ArticleService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @NoAuth
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String keyword) {
        Page<Article> articlePage = articleService.getArticlePage(page, size, category, keyword);
        return Result.success(articlePage);
    }

    @NoAuth
    @GetMapping("/hot")
    public Result<?> hotArticles(@RequestParam(defaultValue = "5") int limit) {
        List<Article> hotList = articleService.getHotArticles(limit);
        return Result.success(hotList);
    }

    @NoAuth
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Article article = articleService.getArticleDetail(id);
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

    @PostMapping("/{id}/like")
    public Result<?> like(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    @GetMapping("/archive")
    public Result<?> archive(@RequestAttribute("userId") Long userId) {
        List<Map<String, Object>> stats = articleService.getArchiveStats(userId);
        return Result.success(stats);
    }
}
