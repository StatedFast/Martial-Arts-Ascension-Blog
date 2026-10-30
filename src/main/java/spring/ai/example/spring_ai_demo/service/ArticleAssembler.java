package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.entity.Article;
import spring.ai.example.spring_ai_demo.entity.ArticleFavorite;
import spring.ai.example.spring_ai_demo.entity.ArticleLike;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.mapper.ArticleFavoriteMapper;
import spring.ai.example.spring_ai_demo.mapper.ArticleLikeMapper;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章视图装配器：补全作者信息 + 当前用户的点赞/收藏状态。
 *
 * 两个关键点：
 * 1) 批量回显：整页文章只发 1 次 IN 查询取「已赞集合」，绝不逐条查（否则就是 N+1）
 * 2) 先复制再装饰：文章列表/详情来自本地缓存，如果直接改缓存里的对象，
 *    A 用户看到的 liked=true 会污染缓存，B 用户打开同一页也会显示已赞。
 */
@Component
public class ArticleAssembler {

    private final UserMapper userMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleFavoriteMapper articleFavoriteMapper;

    public ArticleAssembler(UserMapper userMapper, ArticleLikeMapper articleLikeMapper,
                           ArticleFavoriteMapper articleFavoriteMapper) {
        this.userMapper = userMapper;
        this.articleLikeMapper = articleLikeMapper;
        this.articleFavoriteMapper = articleFavoriteMapper;
    }

    /** 分页缓存安全版：记录一律复制后再装饰 */
    public Page<Article> decoratePage(Page<Article> source, Long currentUserId) {
        if (source == null) {
            return null;
        }
        Page<Article> view = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        view.setRecords(decorateList(source.getRecords(), currentUserId));
        return view;
    }

    /** 列表缓存安全版：返回副本，不动缓存里的原对象 */
    public List<Article> decorateList(List<Article> source, Long currentUserId) {
        if (source == null) {
            return Collections.emptyList();
        }
        List<Article> copies = source.stream().map(this::copy).collect(Collectors.toList());
        decorate(copies, currentUserId);
        return copies;
    }

    public Article decorateSingle(Article source, Long currentUserId) {
        if (source == null) {
            return null;
        }
        Article copy = copy(source);
        decorate(Collections.singletonList(copy), currentUserId);
        return copy;
    }

    /** 原地装饰（调用方必须保证传入的是副本） */
    public void decorate(List<Article> articles, Long currentUserId) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        fillAuthorInfo(articles);
        fillInteractionState(articles, currentUserId);
    }

    public void fillAuthorInfo(List<Article> articles) {
        Set<Long> authorIds = articles.stream()
                .map(Article::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Article article : articles) {
            User author = userMap.get(article.getAuthorId());
            if (author != null) {
                article.setAuthorName(author.getNickname() != null && !author.getNickname().isBlank()
                        ? author.getNickname() : author.getUsername());
                article.setAuthorAvatar(author.getAvatar());
            }
        }
    }

    public void fillInteractionState(List<Article> articles, Long currentUserId) {
        List<Long> ids = articles.stream().map(Article::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return;
        }
        Set<Long> likedIds = queryLikedIds(currentUserId, ids);
        Set<Long> favoritedIds = queryFavoritedIds(currentUserId, ids);
        for (Article article : articles) {
            article.setLiked(likedIds.contains(article.getId()));
            article.setFavorited(favoritedIds.contains(article.getId()));
        }
    }

    private Set<Long> queryLikedIds(Long currentUserId, List<Long> articleIds) {
        if (currentUserId == null) {
            return Collections.emptySet();
        }
        return articleLikeMapper.selectList(new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, currentUserId)
                        .in(ArticleLike::getArticleId, articleIds))
                .stream().map(ArticleLike::getArticleId).collect(Collectors.toSet());
    }

    private Set<Long> queryFavoritedIds(Long currentUserId, List<Long> articleIds) {
        if (currentUserId == null) {
            return Collections.emptySet();
        }
        return articleFavoriteMapper.selectList(new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, currentUserId)
                        .in(ArticleFavorite::getArticleId, articleIds))
                .stream().map(ArticleFavorite::getArticleId).collect(Collectors.toSet());
    }

    private Article copy(Article src) {
        Article a = new Article();
        a.setId(src.getId());
        a.setTitle(src.getTitle());
        a.setContent(src.getContent());
        a.setSummary(src.getSummary());
        a.setCoverImage(src.getCoverImage());
        a.setAuthorId(src.getAuthorId());
        a.setCategory(src.getCategory());
        a.setTags(src.getTags());
        a.setViewCount(src.getViewCount());
        a.setCommentCount(src.getCommentCount());
        a.setLikeCount(src.getLikeCount());
        a.setFavoriteCount(src.getFavoriteCount());
        a.setStatus(src.getStatus());
        a.setIsTop(src.getIsTop());
        a.setCreateTime(src.getCreateTime());
        a.setUpdateTime(src.getUpdateTime());
        a.setDeleted(src.getDeleted());
        return a;
    }
}
