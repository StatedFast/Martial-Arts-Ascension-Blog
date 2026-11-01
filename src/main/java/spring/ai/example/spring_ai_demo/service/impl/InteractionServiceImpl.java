package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.ai.example.spring_ai_demo.dto.InteractionStateDTO;
import spring.ai.example.spring_ai_demo.entity.*;
import spring.ai.example.spring_ai_demo.mapper.*;
import spring.ai.example.spring_ai_demo.service.ArticleAssembler;
import spring.ai.example.spring_ai_demo.service.ArticleService;
import spring.ai.example.spring_ai_demo.service.InteractionService;
import spring.ai.example.spring_ai_demo.service.NotificationService;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InteractionServiceImpl implements InteractionService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ArticleService articleService;
    private final ArticleAssembler articleAssembler;

    public InteractionServiceImpl(ArticleMapper articleMapper, CommentMapper commentMapper,
                                  ArticleLikeMapper articleLikeMapper, ArticleFavoriteMapper articleFavoriteMapper,
                                  CommentLikeMapper commentLikeMapper, UserMapper userMapper,
                                  NotificationService notificationService, ArticleService articleService,
                                  ArticleAssembler articleAssembler) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.articleLikeMapper = articleLikeMapper;
        this.articleFavoriteMapper = articleFavoriteMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.articleService = articleService;
        this.articleAssembler = articleAssembler;
    }

    // ==================== 文章点赞 ====================

    @Override
    @Transactional
    public InteractionStateDTO likeArticle(Long userId, Long articleId) {
        Article article = requirePublishedArticle(articleId);

        boolean inserted = insertIgnoreDuplicate(() -> {
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            return articleLikeMapper.insert(like);
        });

        if (inserted) {
            articleMapper.updateLikeCount(articleId, 1);
            articleService.invalidateCache(articleId);
            notificationService.notify(article.getAuthorId(), userId, Notification.TYPE_LIKE,
                    articleId, null, displayNameOf(userId) + " 点赞了你的文章《" + article.getTitle() + "》");
        }

        Article latest = articleMapper.selectById(articleId);
        return new InteractionStateDTO(true, null,
                latest == null ? 0L : latest.getLikeCount(), null);
    }

    @Override
    @Transactional
    public InteractionStateDTO unlikeArticle(Long userId, Long articleId) {
        requirePublishedArticle(articleId);

        int deleted = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId));

        if (deleted > 0) {
            articleMapper.updateLikeCount(articleId, -1);
            articleService.invalidateCache(articleId);
        }

        Article latest = articleMapper.selectById(articleId);
        return new InteractionStateDTO(false, null,
                latest == null ? 0L : latest.getLikeCount(), null);
    }

    // ==================== 文章收藏 ====================

    @Override
    @Transactional
    public InteractionStateDTO favoriteArticle(Long userId, Long articleId) {
        Article article = requirePublishedArticle(articleId);

        boolean inserted = insertIgnoreDuplicate(() -> {
            ArticleFavorite favorite = new ArticleFavorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(userId);
            return articleFavoriteMapper.insert(favorite);
        });

        if (inserted) {
            articleMapper.updateFavoriteCount(articleId, 1);
            articleService.invalidateCache(articleId);
            notificationService.notify(article.getAuthorId(), userId, Notification.TYPE_FAVORITE,
                    articleId, null, displayNameOf(userId) + " 收藏了你的文章《" + article.getTitle() + "》");
        }

        Article latest = articleMapper.selectById(articleId);
        return new InteractionStateDTO(null, true, null,
                latest == null ? 0L : latest.getFavoriteCount());
    }

    @Override
    @Transactional
    public InteractionStateDTO unfavoriteArticle(Long userId, Long articleId) {
        requirePublishedArticle(articleId);

        int deleted = articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId));

        if (deleted > 0) {
            articleMapper.updateFavoriteCount(articleId, -1);
            articleService.invalidateCache(articleId);
        }

        Article latest = articleMapper.selectById(articleId);
        return new InteractionStateDTO(null, false, null,
                latest == null ? 0L : latest.getFavoriteCount());
    }

    // ==================== 评论点赞 ====================

    @Override
    @Transactional
    public long likeComment(Long userId, Long commentId) {
        Comment comment = requireComment(commentId);

        boolean inserted = insertIgnoreDuplicate(() -> {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            return commentLikeMapper.insert(like);
        });

        if (inserted) {
            commentMapper.updateLikeCount(commentId, 1);
            notificationService.notify(comment.getUserId(), userId, Notification.TYPE_LIKE,
                    comment.getArticleId(), commentId, displayNameOf(userId) + " 点赞了你的评论");
        }

        Comment latest = commentMapper.selectById(commentId);
        return latest == null ? 0L : latest.getLikeCount();
    }

    @Override
    @Transactional
    public long unlikeComment(Long userId, Long commentId) {
        requireComment(commentId);

        int deleted = commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId));

        if (deleted > 0) {
            commentMapper.updateLikeCount(commentId, -1);
        }

        Comment latest = commentMapper.selectById(commentId);
        return latest == null ? 0L : latest.getLikeCount();
    }

    // ==================== 我的收藏 / 我的点赞 ====================

    @Override
    public Page<Article> myFavorites(Long userId, int page, int size) {
        Page<ArticleFavorite> relationPage = articleFavoriteMapper.selectPage(
                new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreateTime));

        List<Long> ids = relationPage.getRecords().stream()
                .map(ArticleFavorite::getArticleId).collect(Collectors.toList());
        return buildArticlePage(relationPage.getCurrent(), relationPage.getSize(),
                relationPage.getTotal(), ids, userId);
    }

    @Override
    public Page<Article> myLikes(Long userId, int page, int size) {
        Page<ArticleLike> relationPage = articleLikeMapper.selectPage(
                new Page<>(page, Math.min(size, 50)),
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .orderByDesc(ArticleLike::getCreateTime));

        List<Long> ids = relationPage.getRecords().stream()
                .map(ArticleLike::getArticleId).collect(Collectors.toList());
        return buildArticlePage(relationPage.getCurrent(), relationPage.getSize(),
                relationPage.getTotal(), ids, userId);
    }

    // ==================== 内部工具 ====================

    /**
     * 关系表插入的幂等封装。
     *
     * 命中唯一索引说明「已经点过赞/收藏过」，对业务而言等价于成功，因此吞掉异常返回 false。
     * MySQL 的 DuplicateKeyException 不会像 PostgreSQL 那样让整个事务进入 aborted 状态，
     * 所以可以在事务内安全捕获，后续的计数更新仍然有效。
     */
    private boolean insertIgnoreDuplicate(Supplier<Integer> insertAction) {
        try {
            return insertAction.get() > 0;
        } catch (DuplicateKeyException e) {
            log.debug("重复的互动请求已被唯一索引拦截: {}", e.getMostSpecificCause().getMessage());
            return false;
        }
    }

    /**
     * 关系表按时间分页后，再按关系顺序装配文章。
     * 注意 selectBatchIds 返回顺序不保证，必须按关系 id 列表重新排序，否则「我的收藏」顺序会乱。
     */
    private Page<Article> buildArticlePage(long current, long size, long total,
                                           List<Long> articleIds, Long currentUserId) {
        if (articleIds.isEmpty()) {
            Page<Article> empty = new Page<>(current, size, total);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Map<Long, Article> articleMap = articleMapper.selectBatchIds(articleIds).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<Article> ordered = articleIds.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Page<Article> result = new Page<>(current, size, total);
        result.setRecords(ordered);
        return articleAssembler.decoratePage(result, currentUserId);
    }

    private Article requirePublishedArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new RuntimeException("文章不存在");
        }
        return article;
    }

    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        return comment;
    }

    private String displayNameOf(Long userId) {
        if (userId == null) {
            return "有人";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "有人";
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }
}
