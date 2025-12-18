package com.product.service;

import com.product.constant.CacheConstants;
import com.product.constant.UserConstants;
import com.product.context.AuthenticationContextHolder;
import com.product.core.redis.RedisCache;
import com.product.entity.LoginUser;
import com.product.entity.SysUser;
import com.product.exception.ServiceException;
import com.product.exception.user.CaptchaException;
import com.product.exception.user.CaptchaExpireException;
import com.product.exception.user.UserNotExistsException;
import com.product.exception.user.UserPasswordNotMatchException;
import com.product.utils.DateUtils;
import com.product.utils.JwtUtils;
import com.product.utils.StringUtils;
import com.product.utils.ip.IpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 登录校验方法
 *
 * @author fast
 */
@Slf4j
@Component
public class SysLoginService
{
    @Autowired
    private JwtUtils jwtUtils;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try
        {
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            // 设置到threadLocal中
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                throw new UserPasswordNotMatchException();
            }
            else
            {
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            // 清理threadLocal
            AuthenticationContextHolder.clearContext();
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token jwt令牌
        return jwtUtils.createToken(loginUser);
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, ""); // uuid = null ? "" : uuid
            log.info("当前缓存中验证码键: {}", redisCache.keys(CacheConstants.CAPTCHA_CODE_KEY));
            String captcha = redisCache.getCacheObject(verifyKey);  // 获取本地缓存中的code
            log.info("当前验证码: {}", captcha);
            if (captcha == null)
            {
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                throw new CaptchaException();
            }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new UserPasswordNotMatchException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        // 获取客户端ip
        sysUser.setLoginIp(IpUtils.getIpAddr());
        // 获取登录时间
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }
}
