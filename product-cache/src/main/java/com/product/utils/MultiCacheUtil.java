package com.product.utils;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 01:17
 * @Description: com.product.utils
 * @version: 1.0
 */
@Component
public class MultiCacheUtil {
    @Autowired
    private Cache<String, Object> caffeineCache; // 刚才配置的 Bean

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 极致性能的查询：L1 -> L2 -> DB
     */
    public <T> T get(String key, Class<T> clazz, Supplier<T> dbLoader) {
        // 1. 先查 L1 (本地内存)
        Object l1Data = caffeineCache.getIfPresent(key);
        if (l1Data != null) {
            return "EMPTY".equals(l1Data) ? null : (T) l1Data;
        }

        // 2. 再查 L2 (Redis)
        T l2Data = (T) redisTemplate.opsForValue().get(key);
        if (l2Data != null) {
            if ("EMPTY".equals(l2Data)) {
                caffeineCache.put(key, "EMPTY");
                return null;
            }
            caffeineCache.put(key, l2Data); // 回填 L1
            return l2Data;
        }

        // 3. 查 DB (这里建议配合之前讲的分布式锁防止击穿)
        T dbData = dbLoader.get();

        // 4. 回填
        if (dbData != null) {
            // Redis 随机过期防雪崩
            redisTemplate.opsForValue().set(key, dbData, 3600 + new Random().nextInt(300), TimeUnit.SECONDS);
            caffeineCache.put(key, dbData);
        } else {
            // 存空值防穿透
            redisTemplate.opsForValue().set(key, "EMPTY", 60, TimeUnit.SECONDS);
            caffeineCache.put(key, "EMPTY");
        }

        return dbData;
    }
}
