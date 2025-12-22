package com.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.entity.SysDictData;
import com.product.mapper.SysDictDataMapper;
import com.product.service.ISysDictDataService;
import com.product.utils.DictUtils;
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
        LambdaQueryWrapper<SysDictData> wrapper = buildWrapper(sysDictData);
        return list(wrapper);
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        LambdaQueryWrapper<SysDictData> wrapper = baseSelectWrapper();
        wrapper.eq(SysDictData::getDictCode, dictCode).last("limit 1");
        return this.getOne(wrapper);
    }

    @Override
    public Page<SysDictData> selectDictDataList(Page<SysDictData> page, SysDictData sysDictData) {
        LambdaQueryWrapper<SysDictData> wrapper = buildWrapper(sysDictData);
        return page(page, wrapper);
    }

    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            return dictDatas;
        }
        dictDatas = getDictDatas(dictType);
        if (StringUtils.isNotEmpty(dictDatas))
        {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return null;
    }

    @Override
    public boolean insertDictData(SysDictData dictData) {
        boolean isSuccess = save(dictData);
        if (isSuccess)
        {
            List<SysDictData> dictDatas = selectDictDataByType(dictData.getDictType());
            DictUtils.setDictCache(dictData.getDictType(), dictDatas);
        }
        return isSuccess;
    }

    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes)
        {
            SysDictData data = selectDictDataById(dictCode);
            removeById(dictCode);
            List<SysDictData> dictDatas = selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    @Override
    public boolean updateDictData(SysDictData dictData) {
        boolean isSuccess = updateById(dictData);
        if (isSuccess)
        {
            List<SysDictData> dictDatas = selectDictDataByType(dictData.getDictType());
            DictUtils.setDictCache(dictData.getDictType(), dictDatas);
        }
        return isSuccess;
    }

    public List<SysDictData> getDictDatas(String dictType) {
        LambdaQueryWrapper<SysDictData> wrapper = baseSelectWrapper();
        wrapper.eq(SysDictData::getStatus, "0")
                .eq(SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictSort);
        return list(wrapper);
    }

    private static LambdaQueryWrapper<SysDictData> baseSelectWrapper() {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                SysDictData::getDictCode, SysDictData::getDictSort, SysDictData::getDictLabel,
                SysDictData::getDictValue, SysDictData::getDictType,
                SysDictData::getCssClass, SysDictData::getListClass, SysDictData::getIsDefault,
                SysDictData::getStatus, SysDictData::getCreateTime
        );
        return wrapper;
    }

    private static LambdaQueryWrapper<SysDictData> buildWrapper(SysDictData sysDictData) {
        LambdaQueryWrapper<SysDictData> wrapper = baseSelectWrapper();
        wrapper.eq(StringUtils.hasText(sysDictData.getDictType()), SysDictData::getDictType, sysDictData.getDictType())
                .like(StringUtils.hasText(sysDictData.getDictLabel()), SysDictData::getDictLabel, sysDictData.getDictLabel())
                .eq(StringUtils.hasText(sysDictData.getStatus()), SysDictData::getStatus, sysDictData.getStatus());
        return wrapper;
    }


}
