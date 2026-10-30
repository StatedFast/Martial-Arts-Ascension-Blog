package spring.ai.example.spring_ai_demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.ai.example.spring_ai_demo.service.AiQuotaService;

/**
 * AI 问答每日配额拦截器。
 *
 * - 管理员（role = ADMIN）直接放行，不计次
 * - 普通用户每天最多 ai.daily-limit 次，超出返回 429 与固定提示
 *
 * 之所以放在拦截器而不在 Controller 里判断：流式接口 produces=text/event-stream，
 * 在 Controller 抛业务异常会撞上内容协商，前端拿不到 JSON 提示；
 * 这里直接写响应体，与 JwtInterceptor / RateLimitInterceptor 的处理方式保持一致。
 */
@Component
public class AiQuotaInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaInterceptor.class);

    private final AiQuotaService quotaService;

    public AiQuotaInterceptor(AiQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 未登录请求没有 userId，交给 JwtInterceptor 处理
        if (!(request.getAttribute("userId") instanceof Long userId)) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        if (quotaService.isAdmin(role)) {
            return true;
        }

        long used = quotaService.consume(userId);
        int limit = quotaService.getDailyLimit();
        if (used > limit) {
            log.info("AI 每日配额用尽: userId={}, used={}, limit={}", userId, used, limit);
            writeQuotaExceeded(response, limit);
            return false;
        }

        log.debug("AI 配额消耗: userId={}, used={}/{}", userId, used, limit);
        return true;
    }

    /** 配额超限响应：前端已按 Result 结构解析并展示 message */
    private void writeQuotaExceeded(HttpServletResponse response, int limit) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429);
        response.getWriter().write(
                "{\"code\":429,\"message\":\"非管理员一天最多" + limit + "次问答\",\"data\":null}");
    }
}
