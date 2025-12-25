package com.product.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Auther: chuan
 * @Date: 2025/12/25 - 12 - 25 - 23:11
 * @Description: com.product.common.annotation
 * @version: 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BizIdPrefix {
    String value();
}
