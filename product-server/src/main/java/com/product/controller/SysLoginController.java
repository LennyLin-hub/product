package com.product.controller;

/**
 * 系统登录控制器
 *
 * @author travel
 * @version 1.0
 * @since 2025-12-15
 *
 * 功能描述：
 * 提供系统登录认证相关功能，包括：
 * - 用户登录验证
 * - 获取用户信息
 * - 获取路由菜单信息
 * - 用户登出功能
 * - 生成验证码
 *
 * API路径：
 * - POST /login - 用户登录
 * - GET /getInfo - 获取当前用户信息
 * - GET /getRouters - 获取路由信息
 * - POST /logout - 用户登出
 * - GET /captchaImage - 获取验证码图片
 *
 * 安全特性：
 * - JWT Token认证
 * - 验证码验证
 * - 密码加密存储
 * - 登录失败锁定机制
 */

import com.product.constant.Constants;
import com.product.core.result.AjaxResult;
import com.product.dto.LoginDTO;
import com.product.entity.LoginUser;
import com.product.entity.SysMenu;
import com.product.entity.SysUser;
import com.product.mapper.SysUserMapper;
import com.product.service.ISysMenuService;
import com.product.service.SysLoginService;
import com.product.service.SysPermissionService;
import com.product.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

;

/**
 * 登录验证
 *
 * @author fast
 */
@Slf4j
@RestController
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 登录方法
     *
     * @param loginDTO 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginDTO loginDTO)
    {
        log.info("用户登录: {}", loginDTO);
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginDTO.getUsername(), loginDTO.getPassword(), loginDTO.getCode(),
                loginDTO.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        log.info("获取用户信息");
        AjaxResult ajax = AjaxResult.success();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        log.info("loginUser: {}", loginUser.toString());
        SysUser sysUser = userMapper.selectUserByUserId(loginUser.getUserId());
        // 获取角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser);
        // 获取权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        ajax.put("user", sysUser);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
