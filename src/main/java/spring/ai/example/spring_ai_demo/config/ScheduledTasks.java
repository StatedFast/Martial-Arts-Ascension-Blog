package spring.ai.example.spring_ai_demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.service.AiQuotaService;
import spring.ai.example.spring_ai_demo.service.ArticleService;

import java.util.Set;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final LocalCacheService cacheService;
    private final ArticleService articleService;
    private final AiQuotaService aiQuotaService;

    public ScheduledTasks(LocalCacheService cacheService, ArticleService articleService,
                          AiQuotaService aiQuotaService) {
        this.cacheService = cacheService;
        this.articleService = articleService;
        this.aiQuotaService = aiQuotaService;
    }

    private static final String VIEW_COUNT_PREFIX = "blog:article:view:";

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

    // 说明：原 syncLikeCounts 定时任务已删除。
    // 点赞数改为由 blog_article_like 唯一关系表作为唯一事实来源，
    // 点赞/取消点赞时用原子 SQL 直接增减 like_count，不再需要异步覆盖式同步。

    @Scheduled(fixedRate = 3600000)
    public void cleanHotArticles() {
        try {
            cacheService.zRemoveRange("blog:article:hot", 0, -101);
            log.debug("热门文章排行榜清理完成");
        } catch (Exception e) {
            log.error("清理热门文章失败: {}", e.getMessage());
        }
    }

    /**
     * AI 每日配额重置：每天 00:05 回收昨天的配额 key。
     * 配额 key 自带「日期 + TTL 到零点」，跨天本就自然归零，
     * 这里只是兜底回收，避免时钟或提前创建的 key 残留占内存。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void resetAiDailyQuota() {
        try {
            int removed = aiQuotaService.cleanOutdated();
            log.info("AI 每日配额已重置，回收 {} 个历史 key", removed);
        } catch (Exception e) {
            log.error("重置 AI 配额失败: {}", e.getMessage());
        }
    }
}
