package spring.ai.example.spring_ai_demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.config.NotificationSseManager;
import spring.ai.example.spring_ai_demo.entity.Notification;
import spring.ai.example.spring_ai_demo.entity.User;
import spring.ai.example.spring_ai_demo.mapper.NotificationMapper;
import spring.ai.example.spring_ai_demo.mapper.UserMapper;
import spring.ai.example.spring_ai_demo.service.NotificationService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /** @提及解析：昵称/用户名只允许中英文、数字、下划线，长度 1~30，避免误吞整句 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{IsHan}\\p{L}\\p{N}_]{1,30})");

    /** 同一个人对同一目标 24 小时内的未读同类通知合并，避免点赞刷屏 */
    private static final int MERGE_WINDOW_HOURS = 24;

    private static final String UNREAD_CACHE_PREFIX = "blog:notify:unread:";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final LocalCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final NotificationSseManager sseManager;

    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper,
                                   LocalCacheService cacheService, ObjectMapper objectMapper,
                                   NotificationSseManager sseManager) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.sseManager = sseManager;
    }

    /**
     * 异步写入通知。
     *
     * 这里是跨 Bean 调用（CommentServiceImpl / InteractionServiceImpl -> 本 Bean），
     * 所以 @Async 能生效；如果把这段逻辑写进调用方自己的类里自调用，@Async 会静默失效。
     */
    @Override
    @Async("taskExecutor")
    public void notify(Long toUserId, Long senderId, String type, Long articleId, Long commentId, String summary) {
        try {
            saveAndPush(toUserId, senderId, type, articleId, commentId, summary);
        } catch (Exception e) {
            // 通知是副作用，失败只记日志，绝不向上抛（否则会污染主业务的事务结果）
            log.warn("写入站内通知失败: to={}, type={}, err={}", toUserId, type, e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    public void notifyMentions(String text, Long senderId, Long articleId, Long commentId, String articleTitle) {
        try {
            List<String> names = parseMentionNames(text);
            if (names.isEmpty()) {
                return;
            }
            // 一次性把这批昵称/用户名对应的用户查出来，避免逐个 LIKE
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1)
                    .and(w -> w.in(User::getNickname, names).or().in(User::getUsername, names)));
            if (users.isEmpty()) {
                return;
            }
            String senderName = nicknameOf(senderId);
            for (User target : users) {
                if (target.getId().equals(senderId)) {
                    continue;
                }
                String title = articleTitle == null ? "" : "《" + articleTitle + "》";
                saveAndPush(target.getId(), senderId, Notification.TYPE_MENTION, articleId, commentId,
                        senderName + " 在" + title + "的评论中 @ 了你");
            }
        } catch (Exception e) {
            log.warn("处理 @提及 通知失败: {}", e.getMessage());
        }
    }

    @Override
    public long unreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String key = UNREAD_CACHE_PREFIX + userId;
        Object cached = cacheService.get(key);
        if (cached instanceof Long value) {
            return value;
        }
        long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        cacheService.set(key, count, 60, TimeUnit.SECONDS);
        return count;
    }

    @Override
    public Page<Notification> page(Long userId, int page, int size, String type, boolean unreadOnly) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(unreadOnly, Notification::getIsRead, 0)
                .eq(type != null && !type.isBlank(), Notification::getType, type)
                .orderByDesc(Notification::getCreateTime);

        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, Math.min(size, 50)), wrapper);
        fillSenderInfo(result.getRecords());
        return result;
    }

    @Override
    public boolean markRead(Long userId, Long notificationId) {
        int rows = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        if (rows > 0) {
            invalidateUnread(userId);
        }
        return rows > 0;
    }

    @Override
    public int markAllRead(Long userId) {
        int rows = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        invalidateUnread(userId);
        return rows;
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        // 连接建立即回推一次未读数，前端无需再额外请求
        return sseManager.subscribe(userId, unreadPayload(userId, null, null));
    }

    // ==================== 内部实现 ====================

    /**
     * 落库 + 推送。刻意做成 private 同步方法：被 @Async 的公开方法调用时不能用 this 再调 @Async 方法。
     */
    private void saveAndPush(Long toUserId, Long senderId, String type, Long articleId,
                             Long commentId, String summary) {
        if (toUserId == null || toUserId.equals(senderId)) {
            // 自己操作自己不发通知
            return;
        }

        String content = summary == null ? "" : summary;
        if (content.length() > 300) {
            content = content.substring(0, 300);
        }

        Notification existing = findMergeable(toUserId, senderId, type, articleId);
        if (existing != null) {
            // 合并：只刷新时间与摘要，不新增行 —— 连点 10 次赞也只产生 1 条未读
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getId, existing.getId())
                    .set(Notification::getCreateTime, LocalDateTime.now())
                    .set(Notification::getContent, content)
                    .set(Notification::getIsRead, 0));
        } else {
            Notification notification = new Notification();
            notification.setUserId(toUserId);
            notification.setSenderId(senderId);
            notification.setType(type);
            notification.setArticleId(articleId);
            notification.setCommentId(commentId);
            notification.setContent(content);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }

        invalidateUnread(toUserId);
        push(toUserId, unreadPayload(toUserId, type, content));
    }

    private Notification findMergeable(Long toUserId, Long senderId, String type, Long articleId) {
        return notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, toUserId)
                .eq(Notification::getSenderId, senderId)
                .eq(Notification::getType, type)
                .eq(articleId != null, Notification::getArticleId, articleId)
                .ge(Notification::getCreateTime, LocalDateTime.now().minusHours(MERGE_WINDOW_HOURS))
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT 1"));
    }

    private List<String> parseMentionNames(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return new ArrayList<>(names);
    }

    private void fillSenderInfo(List<Notification> list) {
        Set<Long> senderIds = list.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Notification n : list) {
            User sender = n.getSenderId() == null ? null : userMap.get(n.getSenderId());
            if (sender != null) {
                n.setSenderName(displayName(sender));
                n.setSenderAvatar(sender.getAvatar());
            } else {
                n.setSenderName("系统");
            }
        }
    }

    private String nicknameOf(Long userId) {
        if (userId == null) {
            return "系统";
        }
        User user = userMapper.selectById(userId);
        return user == null ? "系统" : displayName(user);
    }

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private void invalidateUnread(Long userId) {
        cacheService.delete(UNREAD_CACHE_PREFIX + userId);
    }

    private String unreadPayload(Long userId, String type, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", unreadCount(userId));
        if (type != null) {
            payload.put("type", type);
            payload.put("content", content);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"count\":0}";
        }
    }

    private void push(Long userId, String payload) {
        sseManager.push(userId, payload);
    }
}
