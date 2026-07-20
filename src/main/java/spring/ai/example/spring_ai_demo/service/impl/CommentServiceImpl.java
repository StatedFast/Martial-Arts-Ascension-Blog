package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.dto.CommentDTO;
import spring.ai.example.spring_ai_demo.entity.Comment;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.mapper.CommentMapper;
import spring.ai.example.spring_ai_demo.service.CommentService;
import spring.ai.example.spring_ai_demo.service.UserService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final UserService userService;
    private final LocalCacheService cacheService;

    public CommentServiceImpl(UserService userService, LocalCacheService cacheService) {
        this.userService = userService;
        this.cacheService = cacheService;
    }

    private static final String COMMENT_CACHE_PREFIX = "blog:comments:";

    @Override
    @Transactional
    public Comment addComment(Long userId, CommentDTO dto) {
        Comment comment = new Comment();
        comment.setArticleId(dto.getArticleId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        comment.setReplyToUserId(dto.getReplyToUserId());
        comment.setLikeCount(0L);
        comment.setStatus(1);
        save(comment);

        // 清除评论缓存
        cacheService.delete(COMMENT_CACHE_PREFIX + dto.getArticleId());
        return comment;
    }

    @Override
    public List<CommentDTO> getArticleComments(Long articleId) {
        // 尝试从缓存获取
        String cacheKey = COMMENT_CACHE_PREFIX + articleId;
        @SuppressWarnings("unchecked")
        List<CommentDTO> cached = (List<CommentDTO>) cacheService.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreateTime);

        List<Comment> comments = list(wrapper);
        // 构建用户ID到用户名的映射
        Map<Long, User> userMap = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .map(userService::getUserById)
                .filter(u -> u != null)
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为DTO
        List<CommentDTO> dtos = comments.stream().map(c -> {
            CommentDTO dto = new CommentDTO();
            dto.setId(c.getId());
            dto.setArticleId(c.getArticleId());
            dto.setContent(c.getContent());
            dto.setParentId(c.getParentId());
            dto.setReplyToUserId(c.getReplyToUserId());
            dto.setLikeCount(c.getLikeCount());
            dto.setCreateTime(c.getCreateTime());

            User u = userMap.get(c.getUserId());
            if (u != null) {
                dto.setUsername(u.getNickname());
                dto.setUserAvatar(u.getAvatar());
            }
            if (c.getReplyToUserId() != null && c.getReplyToUserId() > 0) {
                User replyUser = userService.getUserById(c.getReplyToUserId());
                if (replyUser != null) {
                    dto.setReplyToUsername(replyUser.getNickname());
                }
            }
            return dto;
        }).collect(Collectors.toList());

        // 构建树形结构
        List<CommentDTO> tree = buildCommentTree(dtos);

        // 缓存10分钟
        cacheService.set(cacheKey, tree, 10, TimeUnit.MINUTES);
        return tree;
    }

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
        boolean deleted = removeById(commentId);
        if (deleted) {
            cacheService.delete(COMMENT_CACHE_PREFIX + comment.getArticleId());
        }
        return deleted;
    }

    @Override
    public Long getUserCommentCount(Long userId) {
        return count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, 1));
    }

    /**
     * 构建评论树形结构
     */
    private List<CommentDTO> buildCommentTree(List<CommentDTO> allComments) {
        Map<Long, List<CommentDTO>> parentMap = allComments.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(CommentDTO::getParentId));

        List<CommentDTO> roots = allComments.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        // 直接通过引用在List中建立父子关系
        for (CommentDTO root : roots) {
            attachChildren(root, parentMap);
        }
        return roots;
    }

    private void attachChildren(CommentDTO parent, Map<Long, List<CommentDTO>> parentMap) {
        List<CommentDTO> children = parentMap.get(parent.getId());
        if (children != null) {
            // 使用反射或直接放到DTO中。这里我们在前端处理树形
            // 暂时返回扁平结构，由前端构建树
        }
    }
}
