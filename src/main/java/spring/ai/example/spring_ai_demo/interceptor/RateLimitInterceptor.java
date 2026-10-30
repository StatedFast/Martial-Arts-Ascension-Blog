package spring.ai.example.spring_ai_demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.ai.example.spring_ai_demo.annotation.RateLimit;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器。
 *
 * 两个要点：
 * 1) 计数器必须带 TTL：早期实现用永久计数器，窗口过期后计数不归零，
 *    同一个 IP 访问同一路径累计超过阈值就会被永久 429。
 * 2) 支持方法级 @RateLimit：没有注解走默认阈值，有注解按注解的 limit/window 生效。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final LocalCacheService cacheService;

    public RateLimitInterceptor(LocalCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        int limit = DEFAULT_LIMIT;
        int windowSeconds = DEFAULT_WINDOW_SECONDS;
        String message = "请求过于频繁，请稍后再试";

        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {
                limit = rateLimit.limit();
                windowSeconds = (int) Math.max(1, rateLimit.timeUnit().toSeconds(rateLimit.window()));
                message = rateLimit.message();
            }
        }

        String ip = IpUtils.getIpAddress(request);
        String uri = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + uri;

        Long count = cacheService.incrementWithTtl(key, windowSeconds, TimeUnit.SECONDS);

        if (count != null && count > limit) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write(
                    "{\"code\":429,\"message\":\"" + message + "\",\"data\":null}");
            return false;
        }

        return true;
    }
}
