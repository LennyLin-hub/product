package com.product.core.utils;

import com.product.common.constant.Constants;
import com.product.common.constant.HttpStatus;
import com.product.common.utils.StringUtils;
import com.product.core.domain.LoginUser;
import com.product.domain.entity.SysRole;
import com.product.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.PatternMatchUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/// Spring Security安全工具类
/// 该工具类提供了一系列便捷的安全相关方法，用于在业务代码中获取当前用户信息、
/// 验证权限、密码加密等功能。作为Spring Security与业务层之间的桥梁，简化了安全操作的复杂性。
/// 核心功能：
/// 1. 用户信息获取：获取当前登录用户的ID、用户名、部门信息等
/// 2. 认证信息访问：获取Spring Security的Authentication对象
/// 3. 密码处理：提供BCrypt密码加密和验证功能
/// 4. 权限验证：基于注解的权限检查和角色验证
/// 5. 管理员判断：判断用户是否为系统管理员
/// 设计原则：
/// - 工具类设计：所有方法都是静态的，便于直接调用
/// - 异常安全：对可能出现的异常进行统一处理
/// - 便捷性：简化Spring Security复杂API的使用
/// - 一致性：提供统一的错误处理和返回格式
/// 使用场景：
/// - 业务逻辑中需要当前用户信息
/// - 数据权限控制和数据过滤
/// - 操作权限验证和角色判断
/// - 用户注册和密码修改
/// - 审计日志和操作记录
/// 安全特性：
/// - 异常时不会泄露敏感信息
/// - 统一的错误码处理
/// - 支持多种权限验证模式
/// - 密码加密采用BCrypt算法
///
/// @author fast
@Slf4j
public class SecurityUtils
{
    /**
     * 获取当前登录用户的ID
     *
     * 从Spring Security的SecurityContext中获取当前用户的ID。
     * 常用于数据权限控制、操作记录、用户相关的业务处理。
     *
     * @return 当前用户的ID，如果用户未登录则抛出异常
     * @throws ServiceException 当用户未登录或获取用户信息失败时抛出
     */
    public static Long getUserId()
    {
        try
        {
            return getLoginUser().getUserId();
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户ID异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取当前登录用户的用户名
     *
     * 从Spring Security的SecurityContext中获取当前用户的用户名。
     * 常用于日志记录、用户状态检查、个性化显示等场景。
     *
     * @return 当前用户的用户名，如果用户未登录则抛出异常
     * @throws ServiceException 当用户未登录或获取用户名失败时抛出
     */
    public static String getUsername()
    {
        try
        {
            return getLoginUser().getUsername();
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户账户异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取当前登录用户的完整信息
     *
     * 从Spring Security的SecurityContext中获取LoginUser对象，
     * 包含用户的完整信息，如用户ID、用户名、权限列表、角色信息等。
     *
     * @return LoginUser对象，包含当前用户的完整登录信息
     * @throws ServiceException 当用户未登录或获取用户信息失败时抛出
     */
    public static LoginUser getLoginUser()
    {
        try
        {
            return (LoginUser) getAuthentication().getPrincipal();
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户信息异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取Spring Security的Authentication对象
     *
     * 从SecurityContext中获取Authentication对象，该对象包含了
     * 当前用户的认证信息、权限列表、认证详情等。
     *
     * @return Authentication对象，包含用户的认证信息
     */
    public static Authentication getAuthentication()
    {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 使用BCrypt算法加密密码
     *
     * BCrypt是一种强大的密码哈希算法，具有以下特点：
     * - 内置盐值，每次加密结果都不同
     * - 可调整计算强度，增加破解难度
     * - 单向加密，无法反向解密
     * - 抗彩虹表攻击能力强
     *
     * 用于用户注册、密码修改等场景。
     *
     * @param password 原始密码，不能为null
     * @return BCrypt加密后的密码字符串
     */
    public static String encryptPassword(String password)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 验证原始密码与加密密码是否匹配
     *
     * 使用BCrypt算法验证用户输入的密码是否与数据库中存储的
     * 加密密码匹配。用于用户登录验证、密码修改验证等场景。
     *
     * @param rawPassword 原始密码，通常是用户输入的明文密码
     * @param encodedPassword 数据库中存储的BCrypt加密密码
     * @return true表示密码匹配，false表示密码不匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断指定用户ID是否为系统管理员
     *
     * 在本系统中，用户ID为1的用户被认为是系统管理员（超级管理员）。
     * 管理员拥有所有系统的访问权限，不受普通权限限制。
     *
     * @param userId 用户ID
     * @return true表示是管理员，false表示不是管理员
     */
    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }

    /**
     * 验证当前用户是否具备指定权限
     *
     * 基于当前用户的权限列表进行权限验证。支持权限通配符匹配，
     * 如"user:add"可以匹配"user:add"或"user:*"等权限模式。
     *
     * 权限匹配规则：
     * - 精确匹配：权限字符串完全相等
     * - 通配符匹配：支持"*"作为通配符
     * - 超级权限：拥有"*:*"权限的用户拥有所有权限
     *
     * @param permission 需要验证的权限字符串，如"user:add"
     * @return true表示用户具备该权限，false表示不具备该权限
     */
    public static boolean hasPermi(String permission)
    {
        return hasPermi(getLoginUser().getPermissions(), permission);
    }

    /**
     * 验证指定权限列表是否包含某个权限
     *
     * 从给定的权限集合中检查是否包含指定的权限。支持多种匹配模式：
     * - 精确匹配：权限字符串完全相同
     * - 通配符匹配：支持"*"和"**"等通配符
     * - 超级权限："*:*"权限拥有所有权限
     *
     * @param authorities 用户权限列表
     * @param permission 需要检查的权限字符串
     * @return true表示包含该权限，false表示不包含该权限
     */
    public static boolean hasPermi(Collection<String> authorities, String permission)
    {
        return authorities.stream().filter(StringUtils::hasText)
                .anyMatch(x -> Constants.ALL_PERMISSION.equals(x) || PatternMatchUtils.simpleMatch(x, permission));
    }

    /**
     * 验证当前用户是否拥有指定角色
     *
     * 从当前用户的角色列表中检查是否包含指定的角色。
     * 角色验证用于基于角色的访问控制（RBAC）场景。
     *
     * 角色匹配规则：
     * - 精确匹配：角色标识完全相同
     * - 通配符匹配：支持"*"作为通配符
     * - 超级管理员：拥有"admin"角色的用户拥有所有角色权限
     *
     * @param role 需要验证的角色标识，如"admin"、"user"等
     * @return true表示用户拥有该角色，false表示不拥有该角色
     */
    public static boolean hasRole(String role)
    {
        List<SysRole> roleList = getLoginUser().getUser().getRoles();
        Collection<String> roles = roleList.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        return hasRole(roles, role);
    }

    /**
     * 验证指定角色列表是否包含某个角色
     *
     * 从给定的角色集合中检查是否包含指定的角色。
     * 支持角色通配符匹配和超级管理员权限判断。
     *
     * @param roles 用户角色列表
     * @param role 需要检查的角色标识
     * @return true表示包含该角色，false表示不包含该角色
     */
    public static boolean hasRole(Collection<String> roles, String role)
    {
        return roles.stream().filter(StringUtils::hasText)
                .anyMatch(x -> Constants.SUPER_ADMIN.equals(x) || PatternMatchUtils.simpleMatch(x, role));
    }

    public static void recordAuthFailure(String msg, AuthenticationException e) {
        log.error(msg, e);
    }

    public static void recordLogoutEvent(String userName, String msg) {
        log.info(userName, msg);
    }
}
