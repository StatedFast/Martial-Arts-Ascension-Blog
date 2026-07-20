package spring.ai.example.spring_ai_demo.exception;

/**
 * Token已被加入黑名单（用户已登出或Token已被吊销）
 */
public class TokenBlacklistedException extends AuthenticationException {

    public TokenBlacklistedException(String message) {
        super(message);
    }
}
