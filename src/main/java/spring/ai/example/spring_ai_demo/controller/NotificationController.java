package spring.ai.example.spring_ai_demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.entity.Notification;
import spring.ai.example.spring_ai_demo.service.NotificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<?> list(@RequestAttribute("userId") Long userId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String type,
                          @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Page<Notification> result = notificationService.page(userId, page, size, type, unreadOnly);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.success(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public Result<?> markRead(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        return notificationService.markRead(userId, id) ? Result.success() : Result.notFound("通知不存在");
    }

    @PostMapping("/read-all")
    public Result<?> markAllRead(@RequestAttribute("userId") Long userId) {
        return Result.success(Map.of("updated", notificationService.markAllRead(userId)));
    }

    /**
     * 实时通知订阅（SSE）。
     *
     * 浏览器原生 EventSource 不能携带 Authorization 头，所以前端用 fetch + ReadableStream
     * 手动解析 SSE，与 AI 流式对话的用法保持一致。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestAttribute("userId") Long userId) {
        return notificationService.subscribe(userId);
    }
}
