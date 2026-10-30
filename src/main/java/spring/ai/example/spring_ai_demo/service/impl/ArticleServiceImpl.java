package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.dto.ArticleDTO;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.mapper.ArticleMapper;
import spring.ai.example.spring_ai_demo.service.ArticleAssembler;
import spring.ai.example.spring_ai_demo.service.ArticleService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final LocalCacheService cacheService;
    private final ArticleAssembler articleAssembler;

    public ArticleServiceImpl(LocalCacheService cacheService, ArticleAssembler articleAssembler) {
        this.cacheService = cacheService;
        this.articleAssembler = articleAssembler;
    }

    private static final String ARTICLE_CACHE_PREFIX = "blog:article:";
    private static final String ARTICLE_LIST_CACHE_KEY = "blog:article:list:";
    private static final String HOT_ARTICLES_KEY = "blog:article:hot";
    private static final String VIEW_COUNT_KEY = "blog:article:view:";

    @Override
    @Transactional
    public Article publish(Long userId, ArticleDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary() != null ? dto.getSummary()
                : dto.getContent().length() > 200 ? dto.getContent().substring(0, 200) : dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        article.setAuthorId(userId);
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        article.setViewCount(0L);
        article.setCommentCount(0L);
        article.setLikeCount(0L);
        article.setIsTop(0);
        save(article);

        // 事务提交后再清除列表缓存，避免提交前的并发读取重新填充旧数据
        clearListCacheAfterCommit();
        return article;
    }

    @Override
    @Transactional
    public Article updateArticle(Long userId, ArticleDTO dto) {
        Article article = getById(dto.getId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权修改此文章");
        }

        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setCoverImage(dto.getCoverImage());
        article.setCategory(dto.getCategory());
        article.setTags(dto.getTags());
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
        }
        updateById(article);

        // 事务提交后再清除缓存
        clearArticleCacheAfterCommit(article.getId());
        return article;
    }

    @Override
    public Page<Article> getArticlePage(int page, int size, String category, String keyword,
                                        Long currentUserId, String sort) {
        // size 必须限幅：缓存 key 含 size，不限制会被构造大量不同 key 把内存撑爆
        int safeSize = Math.max(1, Math.min(size, 100));
        // 排序归一化：只支持最新/最早两种，其余一律按最新
        boolean earliest = "earliest".equalsIgnoreCase(sort);
        String sortKey = earliest ? "earliest" : "latest";
        // 尝试从缓存获取
        String cacheKey = ARTICLE_LIST_CACHE_KEY + page + ":" + safeSize + ":" +
                (category != null ? category : "all") + ":" + (keyword != null ? keyword : "none") +
                ":" + sortKey;

        @SuppressWarnings("unchecked")
        Page<Article> cached = (Page<Article>) cacheService.get(cacheKey);
        if (cached != null) {
            // 缓存里是公共数据，装饰时统一复制一份，避免把「已赞」状态写回缓存骨架
            return articleAssembler.decoratePage(cached, currentUserId);
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        // 置顶始终在最前；时间排序：latest 最新在前（默认）、earliest 最早在前
        wrapper.eq(Article::getStatus, 1)
                .orderByDesc(Article::getIsTop);
        if (earliest) {
            // 补 id 排序：同一秒创建的多篇也有稳定顺序，避免翻页时重复/漏文章
            wrapper.orderByAsc(Article::getCreateTime).orderByAsc(Article::getId);
        } else {
            wrapper.orderByDesc(Article::getCreateTime).orderByDesc(Article::getId);
        }

        if (category != null && !category.isEmpty()) {
            wrapper.eq(Article::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }

        Page<Article> result = page(new Page<>(page, safeSize), wrapper);

        // 缓存5分钟
        cacheService.set(cacheKey, result, 5, TimeUnit.MINUTES);
        return articleAssembler.decoratePage(result, currentUserId);
    }

    @Override
    public Article getArticleDetail(Long id, Long currentUserId) {
        String cacheKey = ARTICLE_CACHE_PREFIX + id;
        Article cached = (Article) cacheService.get(cacheKey);
        if (cached != null) {
            // 异步增加浏览量
            incrementViewCount(id);
            return articleAssembler.decorateSingle(cached, currentUserId);
        }

        Article article = getById(id);
        if (article == null || article.getStatus() != 1) {
            throw new RuntimeException("文章不存在");
        }

        // 缓存1小时
        cacheService.set(cacheKey, article, 1, TimeUnit.HOURS);

        // 加入热门统计
        cacheService.zIncrementScore(HOT_ARTICLES_KEY, id.toString(), 1);

        incrementViewCount(id);
        return articleAssembler.decorateSingle(article, currentUserId);
    }

    @Override
    public List<Article> getHotArticles(int limit, Long currentUserId) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        // 从缓存有序集合获取热门文章
        Set<LocalCacheService.ZSetEntry> hotSet =
                cacheService.zReverseRangeWithScores(HOT_ARTICLES_KEY, 0, safeLimit - 1);

        List<Article> hotList = new ArrayList<>();
        Set<Long> picked = new HashSet<>();

        if (hotSet != null) {
            for (LocalCacheService.ZSetEntry entry : hotSet) {
                Long articleId = Long.valueOf(entry.getValue());
                Article article = (Article) cacheService.get(ARTICLE_CACHE_PREFIX + articleId);
                if (article == null) {
                    article = getById(articleId);
                    if (article != null) {
                        cacheService.set(ARTICLE_CACHE_PREFIX + articleId, article, 1, TimeUnit.HOURS);
                    }
                }
                if (article != null) {
                    hotList.add(article);
                    picked.add(articleId);
                }
            }
        }

        // 缓存刚预热（只访问过一两篇）时按浏览量兜底补齐，避免榜单只剩一两条
        if (hotList.size() < safeLimit) {
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, 1)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT " + safeLimit * 2);
            for (Article article : list(wrapper)) {
                if (hotList.size() >= safeLimit) {
                    break;
                }
                if (article.getId() != null && picked.add(article.getId())) {
                    hotList.add(article);
                }
            }
        }
        return articleAssembler.decorateList(hotList, currentUserId);
    }

    @Override
    public List<Map<String, Object>> getArchiveStats(Long userId) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, userId)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreateTime);

        List<Article> articles = list(wrapper);
        Map<String, List<Article>> archiveMap = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月");

        for (Article article : articles) {
            String month = article.getCreateTime().format(fmt);
            archiveMap.computeIfAbsent(month, k -> new ArrayList<>()).add(article);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Article>> entry : archiveMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("count", entry.getValue().size());
            item.put("articles", entry.getValue());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean deleteArticle(Long userId, Long articleId) {
        Article article = getById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除此文章");
        }
        boolean deleted = removeById(articleId);
        if (deleted) {
            clearArticleCacheAfterCommit(articleId);
        }
        return deleted;
    }

    @Override
    public Page<Article> getAuthorArticles(Long authorId, int page, int size, Long currentUserId) {
        int safeSize = Math.max(1, Math.min(size, 50));
        Page<Article> result = page(new Page<>(page, safeSize), new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, authorId)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime));
        return articleAssembler.decoratePage(result, currentUserId);
    }

    @Override
    public Page<Article> getMyArticles(Long userId, int page, int size, Integer status) {
        int safeSize = Math.max(1, Math.min(size, 50));
        Page<Article> result = page(new Page<>(page, safeSize), new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId)
                .eq(status != null, Article::getStatus, status)
                .orderByDesc(Article::getCreateTime));
        return articleAssembler.decoratePage(result, userId);
    }

    @Override
    public void invalidateCache(Long articleId) {
        cacheService.delete(ARTICLE_CACHE_PREFIX + articleId);
        clearListCache();
    }

    @Override
    @Async
    public void incrementViewCount(Long articleId) {
        String viewKey = VIEW_COUNT_KEY + articleId;
        Long views = cacheService.increment(viewKey);

        // 每10次浏览同步一次数据库
        if (views != null && views % 10 == 0) {
            Article article = new Article();
            article.setId(articleId);
            article.setViewCount(views);
            updateById(article);
        }
    }

    /**
     * 清除列表缓存（使用通配符匹配）
     */
    private void clearListCache() {
        Set<String> keys = cacheService.keys(ARTICLE_LIST_CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            cacheService.delete(keys);
        }
    }

    /**
     * 事务提交后清除列表缓存。
     * 若在事务提交前清缓存，"已清除但未提交"窗口内的并发读请求
     * 会查到旧数据并回填缓存，导致新发布/修改的文章不展示。
     */
    private void clearListCacheAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearListCache();
            }
        });
    }

    /**
     * 事务提交后清除文章详情缓存与列表缓存
     */
    private void clearArticleCacheAfterCommit(Long articleId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.delete(ARTICLE_CACHE_PREFIX + articleId);
                clearListCache();
            }
        });
    }
}
