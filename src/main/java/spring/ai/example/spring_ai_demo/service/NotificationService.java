package spring.ai.example.spring_ai_demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.ai.example.spring_ai_demo.entity.Notification;

/**
 * 站内通知。
 *
 * 通知一律走「异步 + 失败不影响主流程」：点赞/评论的写入是主链路，
 * 通知只是副作用，通知表写失败不能把用户的评论回滚掉。
 */
public interface NotificationService {

    /**
     * 产生一条通知（异步）。
     *
     * @param toUserId  接收者
     * @param senderId  触发者
     * @param type      见 Notification.TYPE_*
     * @param articleId 跳转用
     * @param commentId 跳转用
     * @param summary   展示摘要（如「张三 评论了你的文章《xxx》」）
     */
    void notify(Long toUserId, Long senderId, String type, Long articleId, Long commentId, String summary);

    /**
     * 解析正文中的 @昵称 并给被提及者发通知（异步）。
     */
    void notifyMentions(String text, Long senderId, Long articleId, Long commentId, String articleTitle);

    long unreadCount(Long userId);

    Page<Notification> page(Long userId, int page, int size, String type, boolean unreadOnly);

    boolean markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);

    /** 订阅实时通知（SSE） */
    SseEmitter subscribe(Long userId);
}
