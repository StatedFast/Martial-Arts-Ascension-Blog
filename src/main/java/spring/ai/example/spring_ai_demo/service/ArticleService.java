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

    /** 分页查询文章列表（含缓存），返回已补全作者与当前用户互动状态的视图 */
    Page<Article> getArticlePage(int page, int size, String category, String keyword, Long currentUserId);

    /** 获取文章详情（含浏览量缓存递增），返回已补全互动状态的视图 */
    Article getArticleDetail(Long id, Long currentUserId);

    /** 获取热门文章（缓存榜单） */
    List<Article> getHotArticles(int limit, Long currentUserId);

    /** 获取文章归档统计 */
    List<Map<String, Object>> getArchiveStats(Long userId);

    /** 某个作者已发布的文章（他人主页） */
    Page<Article> getAuthorArticles(Long authorId, int page, int size, Long currentUserId);

    /** 我发布的文章（含草稿，status 为 null 时不过滤） */
    Page<Article> getMyArticles(Long userId, int page, int size, Integer status);

    /** 删除文章 */
    boolean deleteArticle(Long userId, Long articleId);

    /** 增加浏览量（异步） */
    void incrementViewCount(Long articleId);

    /**
     * 失效文章详情缓存与列表缓存。
     * 点赞/收藏/评论等副作用由其它 Service 触发，统一从这里出口，避免缓存键散落各处。
     */
    void invalidateCache(Long articleId);
}
