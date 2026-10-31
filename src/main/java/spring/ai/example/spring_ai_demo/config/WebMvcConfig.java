package spring.ai.example.spring_ai_demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.ai.example.spring_ai_demo.interceptor.AiQuotaInterceptor;
import spring.ai.example.spring_ai_demo.interceptor.JwtInterceptor;
import spring.ai.example.spring_ai_demo.interceptor.RateLimitInterceptor;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 本地上传文件根目录（与 FileStorageService 同一配置） */
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final AiQuotaInterceptor aiQuotaInterceptor;
    private final ThreadPoolTaskExecutor taskExecutor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, RateLimitInterceptor rateLimitInterceptor,
                        AiQuotaInterceptor aiQuotaInterceptor,
                        @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.jwtInterceptor = jwtInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.aiQuotaInterceptor = aiQuotaInterceptor;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // SSE 流式接口使用业务线程池（默认 SimpleAsyncTaskExecutor 会为每个请求新建线程，不适合生产）
        configurer.setTaskExecutor(taskExecutor);
        // 流式对话由大模型逐字生成，总时长较长，放宽异步超时
        configurer.setDefaultTimeout(120_000);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/refresh-token",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                )
                .order(2);

        // AI 每日配额：必须排在 JwtInterceptor 之后，否则拿不到 userId/role；管理员不限次
        registry.addInterceptor(aiQuotaInterceptor)
                .addPathPatterns("/api/ai/**")
                .order(3);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        // 图片（首页 banner / 默认头像）：站点里体积最大的静态资源，
        // 首次加载后让浏览器直接缓存 30 天，之后完全不再发请求。
        // 注意：换图请改文件名（如 home-songyu-2.jpg）并同步 home.banner-image，
        // 同名替换会让老用户在缓存过期前一直看到旧图。
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        // 用户上传的头像等文件：/uploads/** -> {file.upload-dir}/（磁盘绝对路径，随工作目录解析）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadDir).toAbsolutePath().normalize() + "/");
    }
}
