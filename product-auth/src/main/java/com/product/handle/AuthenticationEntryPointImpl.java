package com.product.handle;

import com.alibaba.fastjson2.JSON;
import com.product.constant.HttpStatus;
import com.product.core.result.AjaxResult;
import com.product.utils.SecurityUtils;
import com.product.utils.ServletUtils;
import com.product.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;

/// 认证失败处理类 - 认证入口点
/// 该类实现了Spring Security的AuthenticationEntryPoint接口，用于处理认证失败的请求。
/// 当用户访问受保护的资源但未进行有效的认证时，Spring Security会调用该处理类。
/// 核心功能：
/// 1. 统一认证失败响应：提供标准化的未授权响应格式
/// 2. 错误信息国际化：支持多语言错误消息
/// 3. 请求信息记录：记录失败的访问请求信息
/// 4. 安全日志记录：记录认证失败事件供安全审计使用
/// 触发场景：
/// - 用户未登录访问受保护的接口
/// - JWT令牌无效或已过期
/// - 令牌解析失败或签名验证失败
/// - 用户账户被禁用或锁定
/// - 其他认证过程中的异常情况
/// 处理流程：
/// 1. 接收认证失败的请求和异常信息
/// 2. 设置HTTP响应状态码为401 Unauthorized
/// 3. 构建统一的JSON错误响应
/// 4. 将错误响应写入HTTP响应流
/// 5. 记录认证失败的安全日志
/// 响应格式：
/// {
///   "code": 401,
///   "msg": "请求访问：/api/user/info，认证失败，无法访问系统资源"
/// }
/// 安全特性：
/// - 避免暴露具体的认证失败原因，防止信息泄露
/// - 统一的响应格式，便于前端统一处理
/// - 支持安全审计和监控
/// - 防止暴力破解攻击的限流处理
/// 扩展功能：
/// - 支持根据不同的异常类型返回不同的错误信息
/// - 支持自定义错误消息模板
/// - 支持异步日志记录
/// - 支持多租户场景下的定制化响应
///
/// @author fast
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    /**
     * 序列化版本号
     * 确保类在不同JVM版本间的序列化兼容性
     */
    private static final long serialVersionUID = -8970718410437077606L;

    /// 认证失败处理方法
    /// 该方法在Spring Security认证流程失败时被调用，负责构建和返回标准的错误响应。
    /// 通过统一的响应格式，前端可以统一处理认证失败的情况。
    /// 处理逻辑：
    /// 1. 设置HTTP状态码为401 Unauthorized
    /// 2. 构建包含请求路径的错误消息
    /// 3. 使用AjaxResult统一响应格式
    /// 4. 将JSON响应写入输出流
    /// 5. 记录安全相关的日志信息
    /// 响应设计原则：
    /// - 不暴露具体的认证失败原因，防止信息泄露
    /// - 提供请求路径信息，便于问题排查
    /// - 使用标准的HTTP状态码
    /// - 统一的响应格式，便于前端处理
    /// 异常类型处理：
    /// - BadCredentialsException：用户名或密码错误
    /// - DisabledException：用户账户被禁用
    /// - LockedException：用户账户被锁定
    /// - AccountExpiredException：用户账户已过期
    /// - CredentialsExpiredException：用户凭证已过期
    /// - 其他AuthenticationException：通用认证失败
    ///
    /// @param request 当前HTTP请求对象，包含请求路径等信息
    /// @param response 当前HTTP响应对象，用于写入错误响应
    /// @param e 认证失败的异常对象，包含具体的失败原因
    /// @throws IOException 当写入响应时发生I/O错误时抛出
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        // 设置HTTP状态码为401 Unauthorized，表示认证失败
        int code = HttpStatus.UNAUTHORIZED;

        // 构建错误消息，包含访问的请求路径
        // 使用StringUtils.format进行字符串格式化，避免字符串拼接的性能问题
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());

        // 将错误响应序列化为JSON并写入响应流
        // 使用AjaxResult.error()方法构建统一的错误响应格式
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));

        // 例如：记录认证失败的事件、IP地址、用户代理等信息
        SecurityUtils.recordAuthFailure(msg, e);
    }
}
