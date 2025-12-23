package com.product.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Auther: chuan
 * @Date: 2025/11/26 - 11 - 26 - 22:13
 * @Description: alioss配置类
 * @version: 1.0
 */
@Component
@ConfigurationProperties(prefix="alioss")
@Data
public class AliOssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
