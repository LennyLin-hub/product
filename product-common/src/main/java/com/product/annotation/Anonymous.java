package com.product.annotation;

import java.lang.annotation.*;

/// 匿名访问注解
/// 该注解用于标记允许匿名访问的Controller类或方法，表示这些URL路径
/// 可以无需用户认证即可访问。与Spring Security的安全配置协同工作，
/// 为特定的接口提供免认证访问的能力。
/// 核心功能：
/// 1. 免认证访问：标记的URL无需登录即可访问
/// 2. 自动配置：与PermitAllUrlProperties配合自动配置Spring Security
/// 3. 灵活控制：支持类级别和方法级别的细粒度控制
/// 4. 路径处理：自动处理路径变量，支持通配符匹配
/// 5. 安全边界：明确界定哪些接口可以公开访问
/// 注解特性：
/// - @Target：支持在类(TYPE)和方法(METHOD)上使用
/// - @Retention：运行时保留，支持反射获取
/// - @Documented：包含在JavaDoc文档中
/// - 无属性：简单标记注解，无需配置参数
/// 使用场景：
/// 1. 用户认证接口：登录、注册、找回密码等
/// 2. 公共API接口：开放给外部系统调用的接口
/// 3. 系统信息接口：获取系统状态、配置信息等
/// 4. 静态资源接口：获取图片、文档等资源
/// 5. 健康检查接口：监控系统使用的状态检查接口
/// 使用方式：
/// - 类级别：整个Controller的所有方法都允许匿名访问
/// - 方法级别：仅特定方法允许匿名访问
/// - 优先级：方法级别注解优先于类级别注解
/// 安全原则：
/// 1. 最小权限原则：只对必要的接口开放匿名访问
/// 2. 仔细审查：定期审查匿名访问接口的安全性
/// 3. 数据保护：确保匿名接口不泄露敏感信息
/// 4. 访问控制：考虑对匿名接口增加频率限制
/// 5. 日志记录：记录匿名访问的操作日志
/// 技术实现：
/// - 通过反射机制扫描注解
/// - 与RequestMappingHandlerMapping集成
/// - 自动生成Spring Security配置
/// - 支持路径变量和通配符
///
/// @author fast
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous
{
    // 该注解为标记注解，无需属性配置
    // 仅用于标识哪些Controller或方法允许匿名访问
}
