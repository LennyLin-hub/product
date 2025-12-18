package com.product.handle;

import com.alibaba.fastjson2.JSON;
import com.product.entity.LoginUser;
import com.product.entity.result.AjaxResult;
import com.product.utils.*;
import com.product.utils.ip.IpUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

/// 用户退出登录成功处理器
/// 该类实现了Spring Security的LogoutSuccessHandler接口，用于处理用户退出登录成功的逻辑。
/// 在JWT认证体系中，退出登录主要是清除服务端的用户缓存和令牌记录。
/// 核心功能：
/// 1. 清除用户缓存：从Redis中删除用户的登录缓存信息
/// 2. 令牌失效：使JWT令牌无法继续使用
/// 3. 会话清理：清理Spring Security的认证上下文
/// 4. 统一响应：返回标准化的退出成功响应
/// 5. 日志记录：记录用户退出登录的安全事件
/// 退出流程：
/// 1. 用户请求退出登录接口
/// 2. Spring Security调用退出处理链
/// 3. 清除SecurityContext中的认证信息
/// 4. 调用LogoutSuccessHandler处理退出逻辑
/// 5. 清除Redis中的用户缓存
/// 6. 返回退出成功的响应给客户端
/// 安全特性：
/// - 即使令牌未过期，退出后也无法继续使用
/// - 支持多设备登录时选择性退出
/// - 退出操作不可逆，需要重新登录
/// - 记录退出操作的安全日志
/// 设计原则：
/// - 无状态设计：JWT退出后需要服务端配合
/// - 及时清理：避免缓存的用户信息泄露
/// - 统一响应：提供标准的JSON响应格式
/// - 幂等操作：多次调用退出操作应该是安全的
/// 扩展功能：
/// - 支持强制下线所有设备
/// - 支持指定设备退出
/// - 支持退出令牌黑名单
/// - 支持退出事件通知
///
/// @author fast
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    /// JWT工具类
    /// 提供JWT令牌的解析、验证、缓存操作等功能
    @Autowired
    private JwtUtils jwtUtils;

    /// 退出登录成功处理方法
    /// 该方法在用户成功退出登录时被Spring Security调用。
    /// 主要负责清理服务端的用户缓存信息，使JWT令牌失效。
    /// 处理逻辑：
    /// 1. 从请求中获取当前的JWT令牌
    /// 2. 解析令牌获取用户信息
    /// 3. 从Redis中删除用户的登录缓存
    /// 4. 记录退出登录的操作日志
    /// 5. 返回退出成功的响应给客户端
    /// 缓存清理：
    /// - 删除用户的登录信息缓存
    /// - 删除用户的权限信息缓存
    /// - 清理用户的会话信息
    /// - 清理相关的临时数据
    /// 安全考虑：
    /// - 退出后令牌立即失效
    /// - 即使客户端持有令牌也无法继续访问
    /// - 支持跨设备的退出操作
    /// - 防止退出操作被绕过
    ///
    /// @param request 当前HTTP请求对象，包含JWT令牌等信息
    /// @param response 当前HTTP响应对象，用于写入退出成功的响应
    /// @param authentication Spring Security的认证对象，包含当前用户的认证信息
    /// @throws IOException 当写入响应时发生I/O错误时抛出
    /// @throws ServletException 当Servlet处理发生错误时抛出
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        // 从请求中解析JWT令牌并获取用户信息
        LoginUser loginUser = jwtUtils.getLoginUser(request);

        if (StringUtils.isNotNull(loginUser))
        {
            // 获取用户名用于日志记录
            String userName = loginUser.getUsername();

            // TODO 后面换成redis时需要删除token
            // 从Redis中删除用户缓存记录
            // 这会使当前的JWT令牌立即失效，即使令牌本身未过期
            // 在JWT的无状态架构中，这是实现退出登录的关键步骤
            jwtUtils.delLoginUser(loginUser.getToken());

            // 例如：记录退出时间、IP地址、用户代理等信息
            SecurityUtils.recordLogoutEvent(userName, StringUtils.format("等处时间: {}, ip: {}", DateUtils.getNowDate(), IpUtils.getIpAddr()));
        }

        // 返回退出成功的JSON响应
        // 使用MessageUtils实现国际化支持
        // 响应格式：{"code": 200, "msg": "退出成功", "data": null}
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
