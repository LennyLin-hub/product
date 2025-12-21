package com.product.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:24
 * @Description: 类SpringCache注解
 * @version: 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRedisCache {
    /**
     * 缓存的Key前缀，支持SpEL表达式
     */
    String key();

    /**
     * 过期时间（秒），默认60秒
     */
    long expire() default 60;

    /**
     * 是否允许缓存空值（解决缓存穿透）
     */
    boolean cacheNull() default true;

    // 新增：是否开启热点保护（分布式锁）
    boolean hotKey() default false;
}
