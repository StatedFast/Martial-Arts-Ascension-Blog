package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.*;
import spring.ai.example.spring_ai_demo.mapper.ArticleMapper;
import spring.ai.example.spring_ai_demo.mapper.CommentLikeMapper;
import spring.ai.example.spring_ai_demo.mapper.CommentMapper;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;
import spring.ai.example.spring_ai_demo.service.ArticleService;
import spring.ai.example.spring_ai_demo.service.CommentService;
import spring.ai.example.spring_ai_demo.service.NotificationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ArticleService articleService;
    private final NotificationService notificationService;

    public CommentServiceImpl(UserMapper userMapper, ArticleMapper articleMapper,
                              CommentLikeMapper commentLikeMapper, ArticleService articleService,
                              NotificationService notificationService) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.articleService = articleService;
        this.notificationService = notificationService;
    }

    // ==================== 发表评论 ====================

    @Override
    @Transactional
    public Comment addComment(Long userId, CommentDTO dto) {
        Article article = articleMapper.selectById(dto.getArticleId());
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new RuntimeException("文章不存在");
        }

        // 楼中楼扁平化：回复的回复统一挂到一级评论下，避免出现无限层级与前端渲染困难
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Long replyToUserId = dto.getReplyToUserId();
        if (parentId > 0) {
            Comment parent = getById(parentId);
            if (parent == null) {
                throw new RuntimeException("要回复的评论不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                parentId = parent.getParentId();
            }
            if (replyToUserId == null) {
                replyToUserId = parent.getUserId();
            }
        } else {
            replyToUserId = null;
        }

        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0L);
        comment.setStatus(1);
        save(comment);

        // 评论数是文章上的冗余计数，必须与真实评论保持一致（原来漏维护，详情页永远显示 0 评论）
        articleMapper.updateCommentCount(article.getId(), 1);
        articleService.invalidateCache(article.getId());

        // 通知与正文解析都放到异步分支，不阻塞评论主链路
        String senderName = displayNameOf(userId);
        if (replyToUserId != null && !replyToUserId.equals(userId)) {
            notificationService.notify(replyToUserId, userId, Notification.TYPE_REPLY,
                    article.getId(), comment.getId(),
                    senderName + " 回复了你在《" + article.getTitle() + "》中的评论");
        } else if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
            notificationService.notify(article.getAuthorId(), userId, Notification.TYPE_COMMENT,
                    article.getId(), comment.getId(),
                    senderName + " 评论了你的文章《" + article.getTitle() + "》");
        }
        notificationService.notifyMentions(dto.getContent(), userId, article.getId(),
                comment.getId(), article.getTitle());

        return comment;
    }

    // ==================== 评论分页 ====================

    @Override
    public Page<CommentDTO> getArticleComments(Long articleId, int page, int size, Long currentUserId) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        // 第一段：一级评论分页（总条数也取自这里，前端据此渲染分页器）
        Page<Comment> rootPage = page(new Page<>(page, safeSize), new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreateTime));

        Page<CommentDTO> result = new Page<>(rootPage.getCurrent(), rootPage.getSize(), rootPage.getTotal());
        List<Comment> roots = rootPage.getRecords();
        if (roots.isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        // 第二段：只取本页一级评论的回复，一次 IN 查询，不分页（单条评论的回复量可控）
        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        List<Comment> children = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .in(Comment::getParentId, rootIds)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreateTime));

        List<Comment> all = new ArrayList<>(roots);
        all.addAll(children);

        Map<Long, User> userMap = loadUsers(all);
        Set<Long> likedIds = loadLikedCommentIds(all, currentUserId);

        Map<Long, List<CommentDTO>> childrenMap = children.stream()
                .map(c -> toDto(c, userMap, likedIds))
                .collect(Collectors.groupingBy(CommentDTO::getParentId));

        List<CommentDTO> dtoRoots = roots.stream()
                .map(c -> {
                    CommentDTO dto = toDto(c, userMap, likedIds);
                    dto.setChildren(childrenMap.getOrDefault(c.getId(), new ArrayList<>()));
                    return dto;
                })
                .collect(Collectors.toList());

        result.setRecords(dtoRoots);
        return result;
    }

    @Override
    public Page<CommentDTO> getUserComments(Long userId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Page<Comment> commentPage = page(new Page<>(page, safeSize), new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime));

        Page<CommentDTO> result = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        if (commentPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        Map<Long, User> userMap = loadUsers(commentPage.getRecords());
        Set<Long> likedIds = loadLikedCommentIds(commentPage.getRecords(), userId);

        // 一次性取回这些评论所属的文章标题
        Set<Long> articleIds = commentPage.getRecords().stream()
                .map(Comment::getArticleId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> titleMap = articleIds.isEmpty() ? Collections.emptyMap()
                : articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, a -> a.getTitle() == null ? "" : a.getTitle()));

        List<CommentDTO> records = commentPage.getRecords().stream()
                .map(c -> {
                    CommentDTO dto = toDto(c, userMap, likedIds);
                    dto.setArticleTitle(titleMap.get(c.getArticleId()));
                    return dto;
                })
                .collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    // ==================== 删除评论 ====================

    @Override
    @Transactional
    public boolean deleteComment(Long userId, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }

        int removed = 1;
        List<Long> removedIds = new ArrayList<>();
        removedIds.add(commentId);

        // 删除一级评论时连带删除其下方的回复，否则这些回复会变成没有父节点的孤儿
        if (comment.getParentId() == null || comment.getParentId() == 0L) {
            List<Comment> children = list(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, commentId));
            if (!children.isEmpty()) {
                removed += children.size();
                removedIds.addAll(children.stream().map(Comment::getId).toList());
                removeByIds(removedIds);
            } else {
                removeById(commentId);
            }
        } else {
            removeById(commentId);
        }

        // 评论点赞关系一并清理，避免关系表残留孤儿行
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .in(CommentLike::getCommentId, removedIds));

        articleMapper.updateCommentCount(comment.getArticleId(), -removed);
        articleService.invalidateCache(comment.getArticleId());
        return true;
    }

    @Override
    public Long getUserCommentCount(Long userId) {
        return count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, 1));
    }

    // ==================== 内部工具 ====================

    /** 一次 IN 查询取回所有评论作者，替代原来逐条 getUserById 的 N+1 */
    private Map<Long, User> loadUsers(List<Comment> comments) {
        Set<Long> userIds = comments.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getUserId(), c.getReplyToUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private Set<Long> loadLikedCommentIds(List<Comment> comments, Long currentUserId) {
        if (currentUserId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        return commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, currentUserId)
                        .in(CommentLike::getCommentId, ids))
                .stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
    }

    private CommentDTO toDto(Comment c, Map<Long, User> userMap, Set<Long> likedIds) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setArticleId(c.getArticleId());
        dto.setUserId(c.getUserId());
        dto.setContent(c.getContent());
        dto.setParentId(c.getParentId());
        dto.setReplyToUserId(c.getReplyToUserId());
        dto.setLikeCount(c.getLikeCount() == null ? 0L : c.getLikeCount());
        dto.setCreateTime(c.getCreateTime());
        dto.setLiked(likedIds.contains(c.getId()));

        User author = userMap.get(c.getUserId());
        if (author != null) {
            dto.setUsername(displayName(author));
            dto.setUserAvatar(author.getAvatar());
        }
        if (c.getReplyToUserId() != null && c.getReplyToUserId() > 0) {
            User replyUser = userMap.get(c.getReplyToUserId());
            if (replyUser != null) {
                dto.setReplyToUsername(displayName(replyUser));
            }
        }
        return dto;
    }

    private String displayName(User user) {
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
    }

    private String displayNameOf(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "有人" : displayName(user);
    }
}
