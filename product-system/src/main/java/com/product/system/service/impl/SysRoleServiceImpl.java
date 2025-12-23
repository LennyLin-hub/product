package com.product.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.domain.entity.SysRole;
import com.product.system.mapper.SysRoleMapper;
import com.product.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Auther: chuan
 * @Date: 2025/12/19 - 12 - 19 - 17:18
 * @Description: com.product.service.impl
 * @version: 1.0
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    @Autowired
    private SysRoleMapper roleMapper;

    @Override
    public List<SysRole> selectRoleAll() {
        return roleMapper.selectRoleList(new SysRole());
    }
}
