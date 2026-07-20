package spring.ai.example.spring_ai_demo.exception;

/**
 * Token无效异常（签名错误、格式错误、被篡改等）
 */
public class TokenInvalidException extends AuthenticationException {

    public TokenInvalidException(String message) {
        super(message);
    }

    public TokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
