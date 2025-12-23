package com.product.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:49
 * @Description: com.product.annotation
 * @version: 1.0
 */
@Target(ElementType.METHOD)
public @interface IpGuard {
    /**
     * 限制的时间窗口（秒），默认 1 分钟
     */
    int seconds() default 60;

    /**
     * 允许的最大访问次数
     */
    int limit() default 20;

    /**
     * 触发限制后，封禁该 IP 的时长（秒），默认封禁 1 小时
     */
    int banTime() default 3600;

    /**
     * 提示语
     */
    String message() default "请求过于频繁，请稍后再试";
}
