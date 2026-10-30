package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import spring.ai.example.spring_ai_demo.dto.InteractionStateDTO;
import spring.ai.example.spring_ai_demo.entity.Article;

/**
 * 互动关系（点赞 / 收藏 / 评论点赞）。
 *
 * 一致性与幂等约定：
 * - 关系表 (目标, 用户) 上建唯一索引，重复提交命中 DuplicateKeyException，直接当成功返回
 * - 计数字段用原子 SQL 增减，只有「关系行真的变化」时才动计数，避免双击导致计数抖动
 */
public interface InteractionService {

    InteractionStateDTO likeArticle(Long userId, Long articleId);

    InteractionStateDTO unlikeArticle(Long userId, Long articleId);

    InteractionStateDTO favoriteArticle(Long userId, Long articleId);

    InteractionStateDTO unfavoriteArticle(Long userId, Long articleId);

    /** 返回评论最新点赞数 */
    long likeComment(Long userId, Long commentId);

    long unlikeComment(Long userId, Long commentId);

    /** 我收藏的文章（按收藏时间倒序） */
    Page<Article> myFavorites(Long userId, int page, int size);

    /** 我点赞的文章（按点赞时间倒序） */
    Page<Article> myLikes(Long userId, int page, int size);
}
