package spring.ai.example.spring_ai_demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 连接注册表：负责维护长连接与心跳，不参与业务。
 *
 * 为什么独立成类：
 * NotificationServiceImpl 实现了接口并被 @Async 代理（JDK 动态代理），
 * 代理对象上只有接口方法；@Scheduled 的 heartbeat 若写在实现类里，
 * 调度器无法在代理上调用它会直接启动失败（"not found in any interface(s) of the exposed proxy type"）。
 * 拆成无接口的独立组件后，调度与连接管理都不再依赖业务 Bean 的代理形态。
 */
@Component
@Slf4j
public class NotificationSseManager {

    /** 单条连接 5 分钟后超时，前端会自动重连，避免僵尸连接长期占用 */
    private static final long SSE_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    /** userId -> 该用户打开的 SSE 连接（同一用户可能开多个标签页） */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 注册一条新连接，并把首帧数据（通常是未读数）直接推给这条连接。
     */
    public SseEmitter subscribe(Long userId, String welcomePayload) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        if (welcomePayload != null) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(welcomePayload));
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
        return emitter;
    }

    /**
     * 推送给某个用户的所有连接。发送失败即视为该连接已断开，直接摘除。
     */
    public void push(Long userId, String payload) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(payload));
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    /**
     * 心跳：定期给活跃连接发注释事件，防止 Nginx / 网关把长时间无数据的连接回收。
     */
    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    public int onlineCount() {
        return emitters.size();
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId);
        }
        log.debug("SSE 连接已移除: userId={}", userId);
    }
}
