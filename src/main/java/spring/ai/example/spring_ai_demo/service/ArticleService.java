package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import spring.ai.example.spring_ai_demo.dto.ArticleDTO;
import spring.ai.example.spring_ai_demo.entity.Article;

import java.util.List;
import java.util.Map;

public interface ArticleService extends IService<Article> {

    /** 发布文章 */
    Article publish(Long userId, ArticleDTO dto);

    /** 更新文章 */
    Article updateArticle(Long userId, ArticleDTO dto);

    /** 分页查询文章列表（含缓存） */
    Page<Article> getArticlePage(int page, int size, String category, String keyword);

    /** 获取文章详情（含浏览量缓存递增） */
    Article getArticleDetail(Long id);

    /** 获取热门文章（Redis缓存） */
    List<Article> getHotArticles(int limit);

    /** 获取文章归档统计 */
    List<Map<String, Object>> getArchiveStats(Long userId);

    /** 删除文章 */
    boolean deleteArticle(Long userId, Long articleId);

    /** 点赞文章 */
    boolean likeArticle(Long articleId);

    /** 增加浏览量（异步） */
    void incrementViewCount(Long articleId);
}
