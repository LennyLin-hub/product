package com.product.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 01:00
 * @Description: com.product.config
 * @version: 1.0
 */
@Configuration
public class MultiCacheConfig {
    @Bean
    public Cache<String, Object> DifferentLocalCache() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(5000) // 本地内存不要开太大，防止 OOM
                .expireAfterWrite(5, TimeUnit.MINUTES) // 建议本地缓存时间设短一点
                .build();
    }
}
