package com.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.product.entity.SysDictData;

import java.util.List;

/**
 * @Auther: chuan
 * @Date: 2025/12/21 - 12 - 21 - 18:45
 * @Description: com.product.service
 * @version: 1.0
 */
public interface ISysDictDataService extends IService<SysDictData> {
    List<SysDictData> selectDictDataList(SysDictData sysDictData);
}
