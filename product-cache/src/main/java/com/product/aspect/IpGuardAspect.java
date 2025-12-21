package com.product.aspect;

import com.product.annotation.IpGuard;
import com.product.utils.ip.IpUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

import static com.product.script.RedisLuaScript.IP_LIMIT_SCRIPT;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:55
 * @Description: com.product.aspect
 * @version: 1.0
 */
@Aspect
@Component
public class IpGuardAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(ipGuard)")
    public Object around(ProceedingJoinPoint joinPoint, IpGuard ipGuard) throws Throwable {
        // 1. 获取 Request 对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return joinPoint.proceed();
        // 2. 提取IP (考虑代理情况)
        String ip = IpUtils.getIpAddr();
        // 获取方法签名作为 Key 的一部分，实现接口级别的限制
        String methodName = joinPoint.getSignature().toShortString();

        String countKey = "ip:count:" + methodName + ":" + ip;
        String banKey = "ip:ban:" + ip;

        // 3. 检查是否已被封禁
        if (Boolean.TRUE.equals(redisTemplate.hasKey(banKey))) {
            throw new RuntimeException(ipGuard.message());
        }

        // 4. 执行 Lua 脚本
        List<String> keys = Arrays.asList(countKey, banKey);
        Long result = redisTemplate.execute(
                IP_LIMIT_SCRIPT,
                keys,
                String.valueOf(ipGuard.limit()),
                String.valueOf(ipGuard.seconds()),
                String.valueOf(ipGuard.banTime())
        );

        if (result != null && result == 0) {
            throw new RuntimeException(ipGuard.message());
        }

        return joinPoint.proceed();
    }
}
