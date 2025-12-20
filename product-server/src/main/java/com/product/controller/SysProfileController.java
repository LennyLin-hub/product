package com.product.controller;

/**
 * 个人信息控制器
 *
 * @author travel
 * @version 1.0
 * @since 2025-12-15
 *
 * 功能描述：
 * 提供用户个人信息管理功能，包括：
 * - 查询个人基本信息
 * - 修改个人资料
 * - 修改用户密码
 * - 头像上传功能
 * - 重置用户密钥
 *
 * API路径：
 * - GET /system/user/profile - 查询个人信息
 * - PUT /system/user/profile - 修改个人信息
 * - PUT /system/user/profile/updatePwd - 修改用户密码
 * - POST /system/user/profile/avatar - 头像上传
 * - GET /system/user/profile/getInfo - 获取用户详细信息
 * - PUT /system/user/profile/resetPwd - 重置用户密码
 *
 * 安全特性：
 * - 密码强度验证
 * - 文件上传安全检查
 * - 用户身份验证
 */

import com.product.core.result.AjaxResult;
import com.product.entity.LoginUser;
import com.product.entity.SysUser;
import com.product.service.ISysUserService;
import com.product.utils.JwtUtils;
import com.product.utils.SecurityUtils;
import com.product.utils.StringUtils;
import com.product.utils.file.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 个人信息 业务处理
 *
 * @author fast
 */
@Slf4j
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        // JWT 中只存了部分字段，这里重新查询完整信息并回写到登录态
        SysUser dbUser = userService.selectUserById(loginUser.getUserId());
        loginUser.setUser(dbUser);
        jwtUtils.setLoginUser(loginUser);
        log.info("获取个人信息: {}", dbUser);
        AjaxResult ajax = AjaxResult.success(dbUser);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser))
        {
            // 更新缓存用户信息
            jwtUtils.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        String userName = loginUser.getUsername();

        // 从数据库获取用户信息，确保密码字段正确
        SysUser user = userService.selectUserByUserName(userName);
        String password = user.getPassword();

        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userName, newPassword))
        {
            // 更新缓存用户密码
            loginUser.getUser().setPassword(newPassword);
            jwtUtils.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = aliOssUtil.uploadAvatar(file);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar))
            {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                jwtUtils.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
