package com.product.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.product.common.utils.uuid.IdUtils;
import org.springframework.stereotype.Component;

/**
 * @Auther: chuan
 * @Date: 2025/12/19 - 12 - 19 - 17:23
 * @Description: 自定义id生成器
 * @version: 1.0
 */
@Component
public class CustomIdGenerator implements IdentifierGenerator {
    @Override
    public Number nextId(Object entity) {
        return Long.getLong(IdUtils.simpleUUID());
    }

    @Override
    public String nextUUID(Object entity) {
        String id = IdUtils.simpleUUID();
        return id;
    }
}
