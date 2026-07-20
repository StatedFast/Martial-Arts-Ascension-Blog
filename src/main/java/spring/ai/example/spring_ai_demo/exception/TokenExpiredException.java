package spring.ai.example.spring_ai_demo.exception;

/**
 * Token已过期异常
 */
public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
