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
        log.info("加密后的密码: {}", s);
        boolean b = SecurityUtils.matchesPassword(newPassword, s);
        log.info("校验结果: {}", b);
    }

}
