package com.product.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.entity.SysDictData;
import com.product.mapper.SysDictDataMapper;
import com.product.service.ISysDictDataService;
import com.product.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Auther: chuan
 * @Date: 2025/12/21 - 12 - 21 - 18:45
 * @Description: com.product.service.impl
 * @version: 1.0
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {
    @Override
    public List<SysDictData> selectDictDataList(SysDictData sysDictData) {
        LambdaQueryChainWrapper<SysDictData> lambdaQueryChainWrapper = getLambdaQueryChainWrapper();
        return lambdaQueryChainWrapper.eq(StringUtils.hasText(sysDictData.getDictType()), SysDictData::getDictType, sysDictData.getDictType())
                .like(StringUtils.hasText(sysDictData.getDictLabel()), SysDictData::getDictLabel, sysDictData.getDictLabel())
                .eq(StringUtils.hasText(sysDictData.getStatus()), SysDictData::getStatus, sysDictData.getStatus())
                .list();
    }

    public LambdaQueryChainWrapper<SysDictData> getLambdaQueryChainWrapper() {
        return Db.lambdaQuery(SysDictData.class).select(
                SysDictData::getDictCode, SysDictData::getDictSort, SysDictData::getDictLabel,
                SysDictData::getDictValue, SysDictData::getDictType,
                SysDictData::getCssClass, SysDictData::getListClass, SysDictData::getIsDefault,
                SysDictData::getStatus, SysDictData::getCreateTime
        );
    }


}
