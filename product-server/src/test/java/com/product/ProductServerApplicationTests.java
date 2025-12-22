package com.product;

import com.product.core.redis.RedisCache;
import com.product.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class ProductServerApplicationTests {
    @Autowired
    private RedisCache redisCache;

    @Test
    void contextLoads() {
        String newPassword = "admin123";
        String s = SecurityUtils.encryptPassword(newPassword);
        boolean b = SecurityUtils.matchesPassword(newPassword, s);
    }

}
