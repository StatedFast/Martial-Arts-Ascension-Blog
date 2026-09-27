package spring.ai.example.spring_ai_demo.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import spring.ai.example.spring_ai_demo.common.Result;
import spring.ai.example.spring_ai_demo.exception.AuthenticationException;
import spring.ai.example.spring_ai_demo.exception.BusinessException;
import spring.ai.example.spring_ai_demo.exception.TokenExpiredException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getMessage());
        return Result.error(404, "资源不存在");
    }

    /**
     * 认证异常处理（Token过期/无效/已吊销）
     */
    @ExceptionHandler(TokenExpiredException.class)
    public Result<?> handleTokenExpired(TokenExpiredException e) {
        log.debug("Token已过期: {}", e.getMessage());
        return Result.error(401, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error(401, e.getMessage());
    }

    /**
     * 业务异常处理（带自定义错误码）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * AI 模型服务调用异常（密钥无效 / 余额不足 / 上游限流 / 网络故障等）
     * 覆盖 Spring AI 重试包装异常与底层 WebClient 响应异常，统一转为友好中文提示
     */
    @ExceptionHandler({WebClientResponseException.class, NonTransientAiException.class, TransientAiException.class})
    public Result<?> handleAiServiceException(RuntimeException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        log.warn("AI 服务调用失败: {}", msg);
        if (msg.contains("401") || msg.contains("Unauthorized") || msg.contains("Authentication")) {
            return Result.error(500, "AI 服务认证失败：请检查 API Key 配置（环境变量 AI_API_KEY）");
        }
        if (msg.contains("402") || msg.contains("Insufficient Balance") || msg.contains("insufficient_quota")) {
            return Result.error(500, "AI 账号余额不足，请充值后重试");
        }
        if (msg.contains("429")) {
            return Result.error(429, "AI 服务请求过于频繁，请稍后再试");
        }
        if (msg.contains("timed out") || msg.contains("timeout") || msg.contains("Timeout")) {
            return Result.error(500, "AI 服务响应超时，请稍后重试");
        }
        return Result.error(500, "AI 服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "服务器内部错误");
    }
}
