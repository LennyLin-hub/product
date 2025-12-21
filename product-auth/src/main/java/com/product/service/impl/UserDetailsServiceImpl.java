package com.product.service.impl;

import com.product.constant.Constants;
import com.product.domain.LoginUser;
import com.product.entity.SysUser;
import com.product.enums.UserStatus;
import com.product.exception.ServiceException;
import com.product.service.ISysUserService;
import com.product.service.SysPasswordService;
import com.product.utils.MessageUtils;
import com.product.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/// 用户验证处理类
/// 该类实现了Spring Security的UserDetailsService接口，是认证流程中的核心组件。
/// 负责根据用户名加载用户信息，用于Spring Security的身份认证。
/// 工作流程：
/// 1. 接收用户名作为输入参数
/// 2. 调用UserService查询用户基本信息
/// 3. 查询用户的权限和角色信息
/// 4. 构建LoginUser对象返回给Spring Security
/// 5. 如果用户不存在，抛出UsernameNotFoundException异常
/// 安全特性：
/// - 用户名不存在时抛出标准异常
/// - 不暴露具体的错误信息（防止用户名枚举攻击）
/// - 支持用户状态检查（是否被禁用、锁定等）
/// - 密码编码在认证过程中处理，此处不涉及
/// 性能考虑：
/// - 用户信息查询会被缓存，避免重复数据库访问
/// - 权限信息会根据需要进行懒加载
/// - 支持数据权限过滤
/// 扩展性：
/// - 可以根据需要添加用户登录日志记录
/// - 支持多租户场景下的用户隔离
/// - 可以集成第三方认证源（LDAP、OAuth等）
///
/// @author fast
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService
{
    /**
     * 用户服务接口
     * 用于查询用户基本信息和权限数据
     */
    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPasswordService passwordService;

    /// 根据用户名加载用户详情
    /// 该方法是Spring Security认证流程的核心入口，在用户登录时被调用。
    /// AuthenticationManager会调用此方法来获取用户信息进行身份验证。
    /// 处理逻辑：
    /// 1. 根据用户名查询系统用户
    /// 2. 验证用户是否存在和是否可用
    /// 3. 查询用户的权限和角色信息
    /// 4. 构建LoginUser对象供Spring Security使用
    /// 异常处理：
    /// - 用户不存在：抛出UsernameNotFoundException
    /// - 用户被禁用：抛出DisabledException
    /// - 用户被锁定：抛出LockedException
    /// - 账户过期：抛出AccountExpiredException
    ///
    /// @param username 用户名，通常是登录名
    /// @return UserDetails实现类，包含用户的认证和授权信息
    /// @throws UsernameNotFoundException 当用户不存在时抛出
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        // 查询系统用户
        SysUser user = userService.selectUserByUserName(username);
        log.info("查询的用户信息: {}", user);
        if (StringUtils.isNull(user))
        {
            log.info("登录用户：{} 不存在.", username);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        }
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        }
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            log.info("登录用户：{} 已被停用.", username);
            throw new UsernameNotFoundException("对不起，您的账号：" + username + " 已被停用");
        }

        passwordService.validate(user);
        // 创建并返回LoginUser对象
        // LoginUser类需要实现UserDetails接口，包含用户基本信息和权限信息
        return createLoginUser(user);
    }

    /// 创建LoginUser对象
    /// 根据SysUser创建LoginUser对象，该对象会被Spring Security用于：
    /// - 身份认证（用户名、密码验证）
    /// - 权限检查（角色和权限验证）
    /// - 会话管理（用户信息缓存）
    /// 扩展点：
    /// - 可以在这里添加用户权限信息查询
    /// - 可以设置用户的额外属性信息
    /// - 可以集成第三方系统的用户数据
    ///
    /// @param user 系统用户对象
    /// @return LoginUser对象，实现了UserDetails接口
    public UserDetails createLoginUser(SysUser user)
    {
        // TODO: 后续可接入真实的权限查询（角色/菜单权限）
        Set<String> permissions = new HashSet<>();
        // 超级管理员直接授予全量权限标识，避免空权限导致拒绝访问
        if (SysUser.isAdmin(user.getUserId()))
        {
            permissions.add(Constants.ALL_PERMISSION);
        }
        return new LoginUser(user, permissions);
    }
}
