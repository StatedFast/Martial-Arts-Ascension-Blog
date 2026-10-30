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
import org.springframework.web.multipart.MultipartFile;
import spring.ai.example.spring_ai_demo.entity.OperationLog;
import spring.ai.example.spring_ai_demo.mapper.OperationLogMapper;
import spring.ai.example.spring_ai_demo.utils.IpUtils;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 核心接口操作日志的专用记录器。
     * 名字与 logback-spring.xml 里的 <logger name="OPERATION_LOG"> 完全对应，
     * 因此这一条日志只会落到 logs/operation/ 下的独立文件，不会污染应用运行日志。
     */
    private static final Logger OPERATION_LOG = LoggerFactory.getLogger("OPERATION_LOG");

    /** JSON 形式的口令类字段："accessToken":"xxx" -> "accessToken":"***"（\x22 即双引号，避免转义地狱） */
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\x22(?:password|oldPassword|newPassword|accessToken|refreshToken|token|secret)\\x22\\s*:\\s*\\x22)[^\\x22]*(\\x22)");

    /** 普通文本形式的敏感字段（如 DTO.toString()） */
    private static final Pattern TEXT_SECRET = Pattern.compile(
            "(?i)((?:password|oldPassword|newPassword|token|secret)\\s*[=:]\\s*)[^,;\\s\\]]+");

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

            // 当前登录人（@NoAuth 公开接口未登录时为 null）
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr != null) {
                oplog.setUserId(Long.valueOf(userIdAttr.toString()));
            }
            Object usernameAttr = request.getAttribute("username");
            if (usernameAttr != null) {
                oplog.setUsername(usernameAttr.toString());
            }

            Object[] args = point.getArgs();
            String params = Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .map(this::stringify)
                    .collect(Collectors.joining(", "));
            // 敏感数据脱敏：密码/Token不记录到日志
            params = maskSensitiveData(params);
            if (params.length() > 500) {
                params = params.substring(0, 500) + "...";
            }
            oplog.setParams(params);

            if (result != null) {
                try {
                    String resultStr = objectMapper.writeValueAsString(result);
                    // 登录/刷新接口的返回体里带 JWT，写进日志文件等于把凭证落盘
                    resultStr = maskSensitiveData(resultStr);
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

            // 先写文件：文件日志是部署后的主要排查手段，不能因为入库失败而丢掉
            writeOperationLogFile(oplog);

            try {
                operationLogMapper.insert(oplog);
            } catch (Exception e) {
                // 入库失败不回流影响业务，文件日志已经落盘
                log.warn("操作日志入库失败（文件日志已记录）: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage());
        }
    }

    /**
     * 单行结构化格式，方便服务器上用 grep / awk 直接过滤：
     * 2026-10-31 10:00:01.123 [http-nio-8081-exec-1] [] INFO  OPERATION_LOG - POST /api/articles/1/like | ArticleController#like | user=2 ip=127.0.0.1 | 12ms | SUCCESS | params=[2, 1]
     */
    private void writeOperationLogFile(OperationLog oplog) {
        try {
            OPERATION_LOG.info("{} {} | {}#{} | user={} ip={} | {}ms | {} | params={} | result={}{}",
                    oplog.getMethod(), oplog.getUrl(), oplog.getModule(), oplog.getOperation(),
                    oplog.getUserId() == null ? "anonymous" : oplog.getUserId(),
                    oplog.getIp(), oplog.getDuration(),
                    oplog.getStatus() != null && oplog.getStatus() == 1 ? "SUCCESS" : "FAILED",
                    oplog.getParams(), oplog.getResult(),
                    oplog.getErrorMsg() == null ? "" : " | error=" + oplog.getErrorMsg());
        } catch (Exception e) {
            log.warn("写操作日志文件失败: {}", e.getMessage());
        }
    }

    private boolean isWriteOperation(ProceedingJoinPoint point) {
        String methodName = point.getSignature().getName();
        return methodName.startsWith("add") || methodName.startsWith("create")
                || methodName.startsWith("update") || methodName.startsWith("delete")
                || methodName.startsWith("remove") || methodName.startsWith("publish")
                || methodName.startsWith("register") || methodName.startsWith("login")
                || methodName.startsWith("change") || methodName.startsWith("upload")
                // 互动类写接口：点赞/收藏/已读
                || methodName.startsWith("like") || methodName.startsWith("unlike")
                || methodName.startsWith("favorite") || methodName.startsWith("unfavorite")
                || methodName.startsWith("mark");
    }

    /**
     * 参数可读化：简单类型直接 toString，DTO 序列化为 JSON（便于排查入参）。
     * 注意序列化后仍会过一遍脱敏，避免 password 原文落盘。
     */
    private String stringify(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof CharSequence || arg instanceof Number || arg instanceof Boolean
                || arg.getClass().isEnum() || arg instanceof MultipartFile) {
            return String.valueOf(arg);
        }
        try {
            return objectMapper.writeValueAsString(arg);
        } catch (Exception e) {
            return String.valueOf(arg);
        }
    }

    /**
     * 敏感数据脱敏（企业级安全）
     * 将密码、Token等敏感信息替换为 ***
     */
    private String maskSensitiveData(String text) {
        if (text == null) return null;
        text = JSON_SECRET.matcher(text).replaceAll("$1***$2");
        text = TEXT_SECRET.matcher(text).replaceAll("$1***");
        return text;
    }
}
