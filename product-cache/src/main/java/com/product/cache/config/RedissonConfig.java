package com.product.cache.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 配置
 *
 * 说明：
 * - 复用项目现有的 spring.data.redis 配置
 * - 仅负责分布式锁、后续可扩展为 Redisson 原生数据结构
 * - 当前缓存读写仍沿用 RedisTemplate，降低改造风险
 */
@Configuration
public class RedissonConfig {
    @Autowired
    private RedisProperties redisProperties;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();

        String host = StringUtils.hasText(redisProperties.getHost()) ? redisProperties.getHost() : "localhost";
        int port = redisProperties.getPort();
        serverConfig.setAddress("redis://" + host + ":" + port);
        serverConfig.setDatabase(redisProperties.getDatabase());

        String password = redisProperties.getPassword();
        if (StringUtils.hasText(password)) {
            serverConfig.setPassword(password);
        }

        Duration timeout = redisProperties.getTimeout();
        if (timeout != null) {
            int timeoutMs = Math.toIntExact(timeout.toMillis());
            serverConfig.setTimeout(timeoutMs);
            serverConfig.setConnectTimeout(timeoutMs);
        }

        return Redisson.create(config);
    }
}
