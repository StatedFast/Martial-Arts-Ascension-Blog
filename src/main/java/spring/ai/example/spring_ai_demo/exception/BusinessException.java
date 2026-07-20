package spring.ai.example.spring_ai_demo.exception;

/**
 * 业务异常基类
 *
 * 用于替代直接抛出RuntimeException，支持：
 * - 自定义错误码
 * - 全局异常处理器统一捕获
 * - 前端根据错误码做差异化处理
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}
