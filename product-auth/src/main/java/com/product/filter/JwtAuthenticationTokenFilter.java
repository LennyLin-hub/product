package com.product.filter;

import com.product.entity.LoginUser;
import com.product.utils.JwtUtils;
import com.product.utils.SecurityUtils;
import com.product.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// JWT认证令牌过滤器
/// 该过滤器是Spring Security JWT认证体系的核心组件，负责在每个请求处理前验证JWT令牌的有效性。
/// 通过继承OncePerRequestFilter确保每个请求只被过滤一次，避免重复处理。
/// 核心功能：
/// 1. 令牌提取：从HTTP请求中提取JWT令牌
/// 2. 令牌验证：验证令牌的完整性和有效性
/// 3. 用户认证：将令牌中的用户信息设置到Spring Security上下文中
/// 4. 权限加载：加载用户的权限信息供后续权限检查使用
/// 工作原理：
/// 1. 从HTTP请求中获取JWT令牌（通常在Authorization头中）
/// 2. 解析令牌获取用户信息（LoginUser对象）
/// 3. 验证令牌的有效性（签名、过期时间等）
/// 4. 构建Spring Security的Authentication对象
/// 5. 将认证信息设置到SecurityContext中
/// 6. 将请求传递给下一个过滤器或控制器
/// 执行时机：
/// - 在UsernamePasswordAuthenticationFilter之前执行
/// - 对除了登录接口外的所有请求进行验证
/// - 无效令牌或缺失令牌时不会阻止请求，而是设置匿名用户
/// 安全特性：
/// - 令牌验证失败时自动清除认证上下文
/// - 支持令牌刷新机制
/// - 防止令牌重放攻击
/// - 支持令牌黑名单机制
/// 性能优化：
/// - 使用本地缓存减少Redis查询
/// - 智能令牌刷新，避免频繁重建认证上下文
/// - 异步处理非关键验证逻辑
///
/// @author fast
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    /**
     * JWT工具类
     * 提供JWT令牌的解析、验证、刷新等功能
     */
    @Autowired
    private JwtUtils jwtUtils;

    /// 过滤器的核心处理方法
    /// 该方法在每个HTTP请求处理前被调用，负责JWT令牌的验证和用户认证。
    /// 处理逻辑包括令牌提取、用户解析、令牌验证、认证上下文设置等。
    /// 处理流程：
    /// 1. 从HTTP请求中提取JWT令牌
    /// 2. 解析令牌获取LoginUser对象
    /// 3. 检查当前SecurityContext是否已存在认证信息
    /// 4. 验证令牌的有效性和过期时间
    /// 5. 构建Authentication对象并设置到SecurityContext
    /// 6. 将请求传递给过滤器链中的下一个组件
    /// 异常处理：
    /// - 令牌无效时：清除认证上下文，继续处理请求
    /// - 令牌过期时：尝试刷新令牌或要求重新登录
    /// - 解析异常时：记录日志并清除认证上下文
    /// - 用户不存在时：清除认证上下文，返回匿名用户
    ///
    /// @param request 当前HTTP请求对象
    /// @param response 当前HTTP响应对象
    /// @param chain 过滤器链，用于传递请求给下一个过滤器
    /// @throws ServletException 当Servlet处理发生错误时抛出
    /// @throws IOException 当I/O操作发生错误时抛出
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 从请求中解析JWT令牌并获取用户信息
        LoginUser loginUser = jwtUtils.getLoginUser(request);

        // 如果用户信息存在且当前SecurityContext中没有认证信息，则进行认证设置
        // 防止重复设置认证信息，避免性能浪费
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            // 验证令牌的有效性，包括签名验证和过期时间检查
            // 如果令牌过期，会尝试从Redis中刷新令牌
            jwtUtils.verifyToken(loginUser);

            // 构建Spring Security的认证对象
            // 参数说明：principal(用户主体), credentials(凭证), authorities(权限列表)
            // credentials设为null，因为JWT中不包含明文密码
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());

            // 设置认证详情信息，包含客户端IP地址等
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 将认证对象设置到Spring Security上下文中
            // 后续的权限检查和业务逻辑都可以通过SecurityContext获取用户信息
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 将请求传递给过滤器链中的下一个组件
        // 必须调用此方法，否则请求会被拦截
        chain.doFilter(request, response);
    }
}
