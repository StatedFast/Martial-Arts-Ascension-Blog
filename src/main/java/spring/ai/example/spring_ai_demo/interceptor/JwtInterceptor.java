package spring.ai.example.spring_ai_demo.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.ai.example.spring_ai_demo.annotation.NoAuth;
import spring.ai.example.spring_ai_demo.exception.TokenExpiredException;
import spring.ai.example.spring_ai_demo.exception.TokenInvalidException;
import spring.ai.example.spring_ai_demo.service.TokenBlacklistService;
import spring.ai.example.spring_ai_demo.utils.JwtUtils;

/**
 * 企业级JWT认证拦截器
 *
 * 功能：
 * - Token签名验证 + 过期检查
 * - Token黑名单检查（支持登出/吊销）
 * - Token类型检查（仅Access Token可访问API）
 * - 细粒度401错误响应（区分过期/无效/已吊销）
 * - @NoAuth接口可选认证（带Token则解析，不带也放行）
 * - 审计日志
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService blacklistService;

    public JwtInterceptor(JwtUtils jwtUtils, TokenBlacklistService blacklistService) {
        this.jwtUtils = jwtUtils;
        this.blacklistService = blacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 非Controller方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = extractToken(request);

        // 标记了 @NoAuth 的接口不需要强制认证
        if (handlerMethod.getMethodAnnotation(NoAuth.class) != null) {
            // 如果携带了有效token，仍然解析并设置用户信息（用于个性化）
            if (token != null) {
                trySetOptionalAuth(request, token);
            }
            return true;
        }

        // 需要认证的接口：检查Token是否存在
        if (token == null || token.isEmpty()) {
            writeErrorResponse(response, 401, "未登录，请先登录");
            return false;
        }

        // 验证Token并设置用户信息
        try {
            Claims claims = jwtUtils.parseToken(token);

            // 检查Token类型：只有access token才能访问API
            String tokenType = jwtUtils.getTokenTypeFromClaims(claims);
            if (!"access".equals(tokenType)) {
                log.warn("使用非Access Token访问API: type={}, jti={}", tokenType, claims.getId());
                writeErrorResponse(response, 401, "无效的Token类型");
                return false;
            }

            // 检查黑名单
            String jti = jwtUtils.getJtiFromClaims(claims);
            if (blacklistService.isBlacklisted(jti)) {
                log.info("已吊销的Token尝试访问: jti={}, uri={}", jti, request.getRequestURI());
                writeErrorResponse(response, 401, "Token已被吊销，请重新登录");
                return false;
            }

            // 设置用户信息到request
            request.setAttribute("userId", jwtUtils.getUserIdFromClaims(claims));
            request.setAttribute("username", jwtUtils.getUsernameFromClaims(claims));
            request.setAttribute("role", jwtUtils.getRoleFromClaims(claims));
            request.setAttribute("jti", jti);

            return true;

        } catch (TokenExpiredException e) {
            writeErrorResponse(response, 401, "Token已过期，请刷新或重新登录");
            return false;
        } catch (TokenInvalidException e) {
            log.warn("无效Token访问: uri={}, reason={}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, 401, "Token无效，请重新登录");
            return false;
        }
    }

    /**
     * 从请求头提取Token（去掉Bearer前缀）
     */
    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    /**
     * @NoAuth接口的可选认证：尝试解析Token，失败也不影响请求
     */
    private void trySetOptionalAuth(HttpServletRequest request, String token) {
        try {
            Claims claims = jwtUtils.parseToken(token);
            String jti = jwtUtils.getJtiFromClaims(claims);

            // 即使是公开接口，黑名单中的Token也不设置用户信息
            if (!blacklistService.isBlacklisted(jti)) {
                request.setAttribute("userId", jwtUtils.getUserIdFromClaims(claims));
                request.setAttribute("username", jwtUtils.getUsernameFromClaims(claims));
                request.setAttribute("role", jwtUtils.getRoleFromClaims(claims));
                request.setAttribute("jti", jti);
            }
        } catch (Exception e) {
            // 公开接口Token无效不影响访问，忽略
        }
    }

    /**
     * 写入JSON错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write(
                "{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
    }
}
