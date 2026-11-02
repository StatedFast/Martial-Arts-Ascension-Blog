package spring.ai.example.spring_ai_demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.ai.example.spring_ai_demo.service.VisitAnalyticsService;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

/**
 * 访问埋点拦截器：只在 HTML 页面路由上记录一次访问（见 WebMvcConfig 注册的路径白名单）。
 *
 * <p>刻意只拦页面、不拦 /api/** 与静态资源：看板要统计的是「有人来读了这一页」，
 * 而不是前端发起的每一次数据请求或每张头像/每个 CSS 的加载，否则 PV 会被资源请求灌水。
 *
 * <p>在请求线程内把 path/ip/ua/来源取成字符串后再交给异步服务落库；preHandle 直接放行，
 * 埋点异常绝不改变页面响应。
 */
@Component
@RequiredArgsConstructor
public class VisitRecordInterceptor implements HandlerInterceptor {

    private final VisitAnalyticsService visitAnalyticsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只统计 GET 页面（POST/PUT 等交互动作不属于「访问」）
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        try {
            String path = request.getRequestURI();
            String ip = IpUtils.getIpAddress(request);
            String ua = request.getHeader("User-Agent");
            String referrer = visitAnalyticsService.normalizeReferrer(request.getHeader("Referer"), request);
            visitAnalyticsService.recordVisit(path, ip, ua, referrer);
        } catch (Exception ignored) {
            // 埋点旁路：任何异常都放行，不影响页面
        }
        return true;
    }
}
