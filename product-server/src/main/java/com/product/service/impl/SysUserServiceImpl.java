package com.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.entity.SysUser;
import com.product.mapper.SysUserMapper;
import com.product.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Auther: chuan
 * @Date: 2025/12/18 - 12 - 18 - 12:27
 * @Description: com.product.service.impl
 * @version: 1.0
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<SysUser> selectUserList(SysUser user) {
        return List.of();
    }

    @Override
    public List<SysUser> selectAllocatedList(SysUser user) {
        return List.of();
    }

    @Override
    public List<SysUser> selectUnallocatedList(SysUser user) {
        return List.of();
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        return sysUserMapper.selectUserByUserName(userName);
    }

    @Override
    public SysUser selectUserById(Long userId) {
        return null;
    }

    @Override
    public String selectUserRoleGroup(String userName) {
        return "";
    }

    @Override
    public String selectUserPostGroup(String userName) {
        return "";
    }

    @Override
    public boolean checkUserNameUnique(SysUser user) {
        return false;
    }

    @Override
    public boolean checkPhoneUnique(SysUser user) {
        return false;
    }

    @Override
    public boolean checkEmailUnique(SysUser user) {
        return false;
    }

    @Override
    public void checkUserAllowed(SysUser user) {

    }

    @Override
    public void checkUserDataScope(Long userId) {

    }

    @Override
    public int insertUser(SysUser user) {
        return 0;
    }

    @Override
    public boolean registerUser(SysUser user) {
        return false;
    }

    @Override
    public int updateUser(SysUser user) {
        return 0;
    }

    @Override
    public void insertUserAuth(Long userId, Long[] roleIds) {

    }

    @Override
    public int updateUserStatus(SysUser user) {
        return 0;
    }

    @Override
    public int updateUserProfile(SysUser user) {
        return 0;
    }

    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return false;
    }

    @Override
    public int resetPwd(SysUser user) {
        return 0;
    }

    @Override
    public int resetUserPwd(String userName, String password) {
        return 0;
    }

    @Override
    public int deleteUserById(Long userId) {
        return 0;
    }

    @Override
    public int deleteUserByIds(Long[] userIds) {
        return 0;
    }

    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
        return "";
    }
}
