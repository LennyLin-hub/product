package com.product.config;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
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

    private static final DefaultIdentifierGenerator ID_GENERATOR = DefaultIdentifierGenerator.getInstance();

    @Override
    public Number nextId(Object entity) {
        // Delegate to MyBatis-Plus default generator to avoid recursion
        return ID_GENERATOR.nextId(entity);

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
