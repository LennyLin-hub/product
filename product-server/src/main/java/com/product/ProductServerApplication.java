package com.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/*
  Spring Boot 在以下情况下会自动开启事务管理：
  - 当类路径中存在 spring-tx 依赖时(spring-boot-starter-web)
  - 当存在 DataSource bean 时
 */
// 使用自定义数据源
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@MapperScan("com.product.**.mapper")
public class ProductServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServerApplication.class, args);
    }

}
