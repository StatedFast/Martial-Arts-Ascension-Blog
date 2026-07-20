package spring.ai.example.spring_ai_demo.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import spring.ai.example.spring_ai_demo.entity.OperationLog;
import spring.ai.example.spring_ai_demo.mapper.OperationLogMapper;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

import java.util.Arrays;

@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public LogAspect(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(* spring.ai.example.spring_ai_demo.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMsg = null;
        Integer status = 1;

        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            status = 0;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 500 || isWriteOperation(point)) {
                saveLog(point, result, errorMsg, status, duration);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, Object result, String errorMsg,
                         Integer status, long duration) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();

            OperationLog oplog = new OperationLog();
            oplog.setModule(point.getTarget().getClass().getSimpleName());
            oplog.setOperation(point.getSignature().getName());
            oplog.setMethod(request.getMethod());
            oplog.setUrl(request.getRequestURI());
            oplog.setIp(IpUtils.getIpAddress(request));

            Object[] args = point.getArgs();
            String params = Arrays.toString(Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .toArray());
            // 敏感数据脱敏：密码/Token不记录到日志
            params = maskSensitiveData(params);
            if (params.length() > 500) {
                params = params.substring(0, 500) + "...";
            }
            oplog.setParams(params);

            if (result != null) {
                try {
                    String resultStr = objectMapper.writeValueAsString(result);
                    if (resultStr.length() > 1000) {
                        resultStr = resultStr.substring(0, 1000) + "...";
                    }
                    oplog.setResult(resultStr);
                } catch (Exception ignored) {
                }
            }

            oplog.setDuration(duration);
            oplog.setStatus(status);
            oplog.setErrorMsg(errorMsg);

            operationLogMapper.insert(oplog);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage());
        }
    }

    private boolean isWriteOperation(ProceedingJoinPoint point) {
        String methodName = point.getSignature().getName();
        return methodName.startsWith("add") || methodName.startsWith("create")
                || methodName.startsWith("update") || methodName.startsWith("delete")
                || methodName.startsWith("remove") || methodName.startsWith("publish")
                || methodName.startsWith("register") || methodName.startsWith("login")
                || methodName.startsWith("change") || methodName.startsWith("upload");
    }

    /**
     * 敏感数据脱敏（企业级安全）
     * 将密码、Token等敏感信息替换为 ***
     */
    private String maskSensitiveData(String text) {
        if (text == null) return null;
        // 脱敏password字段
        text = text.replaceAll("(?i)(password[=: ]+)[^,;\\s\\]]+", "$1***");
        // 脱敏token字段
        text = text.replaceAll("(?i)(token[=: ]+)[^,;\\s\\]]+", "$1***");
        // 脱敏oldPassword/newPassword
        text = text.replaceAll("(?i)(oldPassword[=: ]+)[^,;\\s\\]]+", "$1***");
        text = text.replaceAll("(?i)(newPassword[=: ]+)[^,;\\s\\]]+", "$1***");
        return text;
    }
}
