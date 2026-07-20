package spring.ai.example.spring_ai_demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器（企业级）
 *
 * 功能：
 * - 为每个请求生成唯一RequestId
 * - 放入MDC，所有日志自动携带RequestId
 * - 响应头返回X-Request-Id（方便前端/运维排查问题）
 * - 支持上游传入RequestId（微服务链路追踪）
 *
 * 日志格式中通过 %X{requestId} 引用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 优先使用上游传入的RequestId（微服务场景），否则自动生成
            String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = generateRequestId();
            }

            // 放入MDC，日志自动携带
            MDC.put(MDC_REQUEST_ID, requestId);

            // 响应头返回RequestId
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            chain.doFilter(request, response);
        } finally {
            // 必须清理MDC，防止线程池复用导致数据污染
            MDC.clear();
        }
    }

    /**
     * 生成短格式RequestId（去掉UUID中的横线，取前16位）
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
