package spring.ai.example.spring_ai_demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.service.ArticleService;

import java.util.Set;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final LocalCacheService cacheService;
    private final ArticleService articleService;

    public ScheduledTasks(LocalCacheService cacheService, ArticleService articleService) {
        this.cacheService = cacheService;
        this.articleService = articleService;
    }

    private static final String VIEW_COUNT_PREFIX = "blog:article:view:";
    private static final String LIKE_COUNT_PREFIX = "blog:article:like_count:";

    @Scheduled(fixedRate = 300000)
    public void syncViewCounts() {
        try {
            Set<String> keys = cacheService.keys(VIEW_COUNT_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return;

            for (String key : keys) {
                Long articleId = Long.valueOf(key.replace(VIEW_COUNT_PREFIX, ""));
                Object val = cacheService.get(key);
                if (val != null) {
                    Long viewCount = Long.valueOf(val.toString());
                    Article article = new Article();
                    article.setId(articleId);
                    article.setViewCount(viewCount);
                    articleService.updateById(article);
                }
            }
            log.debug("浏览量同步完成，同步了 {} 篇文章", keys.size());
        } catch (Exception e) {
            log.error("同步浏览量失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 600000)
    public void syncLikeCounts() {
        try {
            Set<String> keys = cacheService.keys(LIKE_COUNT_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return;

            for (String key : keys) {
                Long articleId = Long.valueOf(key.replace(LIKE_COUNT_PREFIX, ""));
                Object val = cacheService.get(key);
                if (val != null) {
                    Long likeCount = Long.valueOf(val.toString());
                    Article article = new Article();
                    article.setId(articleId);
                    article.setLikeCount(likeCount);
                    articleService.updateById(article);
                }
            }
            log.debug("点赞数同步完成，同步了 {} 篇文章", keys.size());
        } catch (Exception e) {
            log.error("同步点赞数失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanHotArticles() {
        try {
            cacheService.zRemoveRange("blog:article:hot", 0, -101);
            log.debug("热门文章排行榜清理完成");
        } catch (Exception e) {
            log.error("清理热门文章失败: {}", e.getMessage());
        }
    }
}
