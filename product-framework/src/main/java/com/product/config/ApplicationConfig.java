package com.product.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.TimeZone;

/**
 * Spring Boot应用程序核心配置类
 *
 * 该配置类负责应用程序的基础设施配置，包括：
 * 1. MyBatis Mapper接口扫描配置
 * 2. AOP代理对象暴露配置（支持SpringUtils.getAopProxy()方法）
 * 3. Jackson JSON序列化时区配置
 *
 * 这是应用程序启动时加载的核心配置之一，确保各组件能够正常工作。
 *
 * @author fast
 * @version 1.0
 * @since 1.0
 */
@Configuration
/**
 * 启用Spring AOP代理功能
 *
 * 通过exposeProxy=true配置，允许通过AopContext.currentProxy()获取当前对象的代理。
 * 这个配置对于在同一个类中调用带有事务注解或切面注解的方法时非常重要，
 * 可以确保AOP功能正常工作（如事务、缓存、日志等切面）。
 *
 * @see com.product.utils.spring.SpringUtils#getAopProxy(Object)
 */
@EnableAspectJAutoProxy(exposeProxy = true)
/**
 * MyBatis Mapper接口自动扫描配置
 *
 * 扫描指定路径下的Mapper接口，自动创建代理类并注册到Spring容器中。
 * 使用"com.**.mapper"通配符模式，支持扫描所有com包及其子包下的mapper目录。
 *
 * 扫描范围示例：
 * - com.product.mapper.*
 * - com.product.system.mapper.*
 * - com.example.user.mapper.*
 *
 * 这样配置的好处是可以自动发现所有Mapper接口，无需手动注册。
 */
@MapperScan("com.**.mapper")
public class ApplicationConfig
{
    /**
     * Jackson JSON序列化时区配置Bean
     *
     * 配置Jackson在序列化和反序列化日期时间类型时使用的时区。
     * 使用系统默认时区确保时间处理的准确性，避免因时区问题导致的时间错乱。
     *
     * 该配置会影响以下场景：
     * 1. REST API响应中的日期时间字段格式化
     * 2. 接收请求中的日期时间参数解析
     * 3. 数据库时间字段的JSON序列化
     *
     * @return Jackson2ObjectMapperBuilderCustomizer 时区配置定制器
     *
     * @example 配置生效后的效果
     *          如果系统时区为Asia/Shanghai，那么所有JSON中的时间都会按照北京时间处理
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization()
    {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }
}
