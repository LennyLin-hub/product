package com.product;

import com.product.constant.CacheConstants;
import com.product.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;

@SpringBootTest
class ProductServerApplicationTests {
    @Autowired
    private RedisCache redisCache;

    @Test
    void contextLoads() {
        Collection<String> keys = redisCache.keys(CacheConstants.CAPTCHA_CODE_KEY);
        System.out.println(keys);
    }

}
