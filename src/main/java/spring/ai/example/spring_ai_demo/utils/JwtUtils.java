package spring.ai.example.spring_ai_demo.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import spring.ai.example.spring_ai_demo.exception.TokenExpiredException;
import spring.ai.example.spring_ai_demo.exception.TokenInvalidException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 企业级JWT工具类
 *
 * 特性：
 * - 标准Claims（jti/iss/aud/iat/exp/nbf）
 * - 显式指定HS256算法，防止算法混淆攻击
 * - 时钟偏差容忍（分布式环境）
 * - 细粒度异常（过期/签名错误/格式错误）
 * - Access Token + Refresh Token 双令牌机制
 * - 审计日志
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:1800000}")
    private Long accessTokenExpiration; // Access Token 有效期，默认30分钟

    @Value("${jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenExpiration; // Refresh Token 有效期，默认7天

    @Value("${jwt.issuer:spring-blog}")
    private String issuer;

    @Value("${jwt.audience:blog-client}")
    private String audience;

    @Value("${jwt.clock-skew-seconds:60}")
    private Long clockSkewSeconds; // 时钟偏差容忍，默认60秒

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT密钥长度不足256位(32字节)，当前长度: " + keyBytes.length + "，请配置更长的密钥");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ==================== Token 生成 ====================

    /**
     * 生成 Access Token（短期，用于接口认证）
     */
    public String generateAccessToken(Long userId, String username, String role) {
        String jti = UUID.randomUUID().toString();
        String token = buildToken(userId, username, role, jti, accessTokenExpiration, "access");
        log.debug("签发AccessToken: userId={}, jti={}, 有效期={}ms", userId, jti, accessTokenExpiration);
        return token;
    }

    /**
     * 生成 Refresh Token（长期，仅用于刷新Access Token）
     */
    public String generateRefreshToken(Long userId, String username) {
        String jti = UUID.randomUUID().toString();
        String token = buildToken(userId, username, null, jti, refreshTokenExpiration, "refresh");
        log.debug("签发RefreshToken: userId={}, jti={}, 有效期={}ms", userId, jti, refreshTokenExpiration);
        return token;
    }

    /**
     * 构建Token（内部方法）
     */
    private String buildToken(Long userId, String username, String role,
                              String jti, Long expiration, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenType", tokenType);
        if (role != null) {
            claims.put("role", role);
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(jti)                                    // jti: Token唯一标识
                .issuer(issuer)                             // iss: 签发者
                .audience().add(audience).and()             // aud: 受众
                .subject(username)                          // sub: 主题
                .claims(claims)                             // 自定义Claims
                .issuedAt(now)                              // iat: 签发时间
                .notBefore(now)                             // nbf: 生效时间
                .expiration(expiryDate)                     // exp: 过期时间
                .signWith(getSigningKey(), Jwts.SIG.HS256)  // 显式指定HS256算法
                .compact();
    }

    // ==================== Token 解析与验证 ====================

    /**
     * 解析Token获取Claims（带完整验证）
     *
     * @throws TokenExpiredException  Token已过期
     * @throws TokenInvalidException  Token无效（签名错误/格式错误/被篡改）
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())            // 验证签名
                    .requireIssuer(issuer)                  // 验证签发者
                    .requireAudience(audience)              // 验证受众
                    .clockSkewSeconds(clockSkewSeconds)     // 时钟偏差容忍
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期: jti={}, expiredAt={}", e.getClaims().getId(), e.getClaims().getExpiration());
            throw new TokenExpiredException("Token已过期，请刷新或重新登录", e);
        } catch (SignatureException e) {
            log.warn("Token签名验证失败，可能被篡改: {}", e.getMessage());
            throw new TokenInvalidException("Token签名无效", e);
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            throw new TokenInvalidException("Token格式无效", e);
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token类型: {}", e.getMessage());
            throw new TokenInvalidException("不支持的Token类型", e);
        } catch (IllegalArgumentException e) {
            log.warn("Token参数异常: {}", e.getMessage());
            throw new TokenInvalidException("Token为空或无效", e);
        }
    }

    /**
     * 验证Token是否有效（不抛异常版本，用于@NoAuth接口可选认证）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Claims 提取 ====================

    /**
     * 获取Token唯一标识（jti）
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 获取角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 获取Token类型（access / refresh）
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenType", String.class);
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    public long getRemainingMillis(String token) {
        Date expiration = getExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 从Claims中安全提取信息（不重新解析，用于已解析的场景）
     */
    public Long getUserIdFromClaims(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromClaims(Claims claims) {
        return claims.getSubject();
    }

    public String getRoleFromClaims(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getJtiFromClaims(Claims claims) {
        return claims.getId();
    }

    public String getTokenTypeFromClaims(Claims claims) {
        return claims.get("tokenType", String.class);
    }
}
