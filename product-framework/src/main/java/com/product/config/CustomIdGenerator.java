package com.product.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.product.common.annotation.BizIdPrefix;
import com.product.common.utils.StringUtils;
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
        // 使用 MyBatis-Plus 提供的雪花算法，确保数值型主键可用
        return IdWorker.getId();
    }

    @Override
    public String nextUUID(Object entity) {
        String prefix = getPrefix(entity);
        String suffix = IdUtils.simpleUUID();
        if (StringUtils.isNotEmpty(prefix)) {
            return prefix + suffix;
        }
        return suffix;
    }

    private String getPrefix(Object entity) {
        BizIdPrefix annotation = entity.getClass().getAnnotation(BizIdPrefix.class);
        if (annotation != null) {
            return annotation.value();
        }
        return null;
    }
}
