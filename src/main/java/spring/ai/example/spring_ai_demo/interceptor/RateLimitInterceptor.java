package spring.ai.example.spring_ai_demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.ai.example.spring_ai_demo.config.LocalCacheService;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LocalCacheService cacheService;

    public RateLimitInterceptor(LocalCacheService cacheService) {
        this.cacheService = cacheService;
    }

    private static final int DEFAULT_LIMIT = 50;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = IpUtils.getIpAddress(request);
        String uri = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + uri;

        Long count = cacheService.increment(key);

        if (count != null && count > DEFAULT_LIMIT) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }
}
