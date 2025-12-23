package com.product.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:15
 * @Description: com.product.config
 * @version: 1.0
 */
@Configuration
@EnableCaching // 开启缓存支持
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 1. 创建并配置 ObjectMapper
        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 新版开启类型支持的方式
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 2. 使用新的构造函数注入 ObjectMapper
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // 3. 设置序列化规则
        template.setKeySerializer(RedisSerializer.string()); // 简写方式
        template.setHashKeySerializer(RedisSerializer.string());

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory,
                                            MessageListenerAdapter adapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(adapter, new PatternTopic("CACHE_INVALIDATE_TOPIC"));
        return container;
    }

    @Bean
    MessageListenerAdapter adapter(CacheReceiver receiver) {
        // 收到消息后调用 receiver 的 handle 方法
        return new MessageListenerAdapter(receiver, "handle");
    }

    @Component
    class CacheReceiver {
        @Autowired
        private Cache<String, Object> caffeineCache;

        public void handle(String key) {
            caffeineCache.invalidate(key); // 清除本地缓存
        }
    }
}
