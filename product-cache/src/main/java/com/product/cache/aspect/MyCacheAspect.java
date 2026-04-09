package com.product.cache.aspect;

import com.product.cache.annotation.MyRedisCache;
import com.product.cache.constants.RedisLockKeys;
import com.product.cache.lock.RedisDistributedLock;
import com.product.cache.utils.SpelParser;
import org.redisson.api.RLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:38
 * @Description: com.product.aspect
 * @version: 1.0
 */
@Aspect
@Component
public class MyCacheAspect {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Around("@annotation(myRedisCache)")
    public Object around(ProceedingJoinPoint joinPoint, MyRedisCache myRedisCache) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        // 1. 解析 Key
        String realKey = SpelParser.parse(myRedisCache.key(), signature.getMethod(), args);

        // 2. 先尝试从缓存获取
        Object cacheData = redisTemplate.opsForValue().get(realKey);
        if (cacheData != null) {
            return "EMPTY_VALUE".equals(cacheData) ? null : cacheData;
        }

        // 3. 判断是否需要开启热点 Key 分布式锁
        if (myRedisCache.hotKey()) {
            return getWithLock(realKey, joinPoint, myRedisCache);
        } else {
            return getAndSave(realKey, joinPoint, myRedisCache);
        }
    }

    /**
     * 带分布式锁的获取方式（解决热点Key击穿）
     */
    private Object getWithLock(String realKey, ProceedingJoinPoint joinPoint, MyRedisCache myRedisCache) throws Throwable {
        String lockKey = RedisLockKeys.Cache.hotKey(realKey);
        RLock lock = redisDistributedLock.getLock(lockKey);

        // 采用 Redisson 锁，避免手写 SETNX + Lua 解锁的并发边界问题
        lock.lock();
        try {
            // 双重检查缓存 (Double Check)
            Object data = redisTemplate.opsForValue().get(realKey);
            if (data != null) {
                return "EMPTY_VALUE".equals(data) ? null : data;
            }

            // 拿到锁后查询数据库并保存
            return getAndSave(realKey, joinPoint, myRedisCache);
        } finally {
            redisDistributedLock.unlock(lock);
        }
    }

    /**
     * 普通获取方式（不加锁）
     */
    private Object getAndSave(String realKey, ProceedingJoinPoint joinPoint, MyRedisCache myRedisCache) throws Throwable {
        Object dbData = joinPoint.proceed();

        long expireTime = myRedisCache.expire() + new Random().nextInt(30); // 防雪崩随机时间

        if (dbData != null) {
            redisTemplate.opsForValue().set(realKey, dbData, expireTime, TimeUnit.SECONDS);
        } else if (myRedisCache.cacheNull()) {
            // 防穿透
            redisTemplate.opsForValue().set(realKey, "EMPTY_VALUE", 5, TimeUnit.MINUTES);
        }
        return dbData;
    }
}
