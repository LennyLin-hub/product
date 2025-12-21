package com.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.product.entity.SysMenu;
import com.product.vo.RouterVo;

import java.util.List;

/**
 * @Auther: chuan
 * @Date: 2025/12/19 - 12 - 19 - 17:17
 * @Description: com.product.service
 * @version: 1.0
 */
public interface ISysMenuService extends IService<SysMenu> {
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    List<RouterVo> buildMenus(List<SysMenu> menus);
}
