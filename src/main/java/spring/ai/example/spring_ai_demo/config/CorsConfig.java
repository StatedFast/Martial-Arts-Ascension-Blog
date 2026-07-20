package spring.ai.example.spring_ai_demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS跨域安全配置（企业级）
 *
 * 特性：
 * - 白名单域名控制（生产环境禁止使用 * ）
 * - 仅允许必要的HTTP方法
 * - 仅暴露必要的响应头
 * - 支持预检请求缓存（减少OPTIONS请求）
 * - 通过配置文件管理允许的域名
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的域名（生产环境通过配置文件指定）
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.forEach(origin -> config.addAllowedOriginPattern(origin.trim()));

        // 允许的请求头
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Request-Id");
        config.addAllowedHeader("X-Requested-With");

        // 允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // 暴露给前端的响应头
        config.addExposedHeader("X-Request-Id");

        // 允许携带Cookie/认证信息
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒），减少OPTIONS请求
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
