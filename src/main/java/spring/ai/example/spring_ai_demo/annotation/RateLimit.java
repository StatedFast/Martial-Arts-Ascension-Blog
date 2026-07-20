package spring.ai.example.spring_ai_demo.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限制时间内允许的请求次数 */
    int limit() default 10;

    /** 限制时间窗口 */
    int window() default 1;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 限流提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
