package com.product.pps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.domain.dto.BatchSearchDTO;
import com.product.domain.entity.ProductionBatch;
import com.product.domain.vo.ProductionBatchVO;
import com.product.pps.mapper.ProductionBatchMapper;
import com.product.pps.service.IProductionBatchService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

/**
 * 生产批次（订单行拆批）Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-27
 */
@Service
public class ProductionBatchServiceImpl extends ServiceImpl<ProductionBatchMapper, ProductionBatch> implements IProductionBatchService {
    @Autowired
    private ProductionBatchMapper productionBatchMapper;

    /**
     * 查询生产批次（订单行拆批）
     *
     * @param batchId 生产批次（订单行拆批）主键
     * @return 生产批次（订单行拆批）
     */
    @Override
    public ProductionBatch selectProductionBatchByBatchId(String batchId) {
        return getById(batchId);
    }

    /**
     * 查询生产批次（订单行拆批）列表
     *
     * @param productionBatch 查询条件
     * @return 生产批次（订单行拆批）集合
     */
    @Override
    public List<ProductionBatch> selectProductionBatchList(ProductionBatch productionBatch) {
        return list(buildQueryWrapper(productionBatch));
    }

    /**
     * 分页查询生产批次（订单行拆批）列表
     *
     * @param page           分页参数
     * @param batchSearchDTO 查询条件
     * @return 分页结果
     */
    @Override
    public Page<ProductionBatchVO> selectProductionBatchPage(Page<ProductionBatchVO> page, BatchSearchDTO batchSearchDTO) {
        return productionBatchMapper.selectProductionBatchPage(page, batchSearchDTO);
    }

    /**
     * 新增生产批次（订单行拆批）
     *
     * @param productionBatch 生产批次（订单行拆批）
     * @return 是否成功
     */
    @Override
    public boolean insertProductionBatch(ProductionBatch productionBatch) {
        boolean saved = save(productionBatch);
        return saved;
    }

    /**
     * 批量新增生产批次（订单行拆批）
     *
     * @param productionBatchs 生产批次（订单行拆批）列表
     * @return 成功条数
     */
    @Override
    public int batchInsertProductionBatch(List<ProductionBatch> productionBatchs) {
        if (CollectionUtils.isEmpty(productionBatchs)) {
            return 0;
        }
        boolean success = saveBatch(productionBatchs);
        return success ? productionBatchs.size() : 0;
    }

    /**
     * 修改生产批次（订单行拆批）
     *
     * @param productionBatch 生产批次（订单行拆批）
     * @return 是否成功
     */
    @Override
    public boolean updateProductionBatch(ProductionBatch productionBatch) {
        boolean updated = updateById(productionBatch);
        return updated;
    }

    /**
     * 批量删除生产批次（订单行拆批）
     *
     * @param batchIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteProductionBatchByBatchIds(String[] batchIds) {
        if (batchIds == null || batchIds.length == 0) {
            return false;
        }
        return removeByIds(Arrays.asList(batchIds));
    }

    /**
     * 删除生产批次（订单行拆批）信息
     *
     * @param batchId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteProductionBatchByBatchId(String batchId) {
        return removeById(batchId);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<ProductionBatch> buildQueryWrapper(ProductionBatch productionBatch) {
        LambdaQueryWrapper<ProductionBatch> wrapper = new LambdaQueryWrapper<>();
        if (productionBatch == null) {
            return wrapper;
        }
        wrapper.eq(productionBatch.getOrderLineId() != null, ProductionBatch::getOrderLineId, productionBatch.getOrderLineId());
        wrapper.eq(productionBatch.getBatchQty() != null, ProductionBatch::getBatchQty, productionBatch.getBatchQty());
        wrapper.eq(productionBatch.getStatus() != null, ProductionBatch::getStatus, productionBatch.getStatus());
        Object beginPlannedStart = productionBatch.getParams().get("beginPlannedStart");
        Object endPlannedStart = productionBatch.getParams().get("endPlannedStart");
        wrapper.between(beginPlannedStart != null && endPlannedStart != null, ProductionBatch::getPlannedStart, beginPlannedStart, endPlannedStart);
        Object beginPlannedEnd = productionBatch.getParams().get("beginPlannedEnd");
        Object endPlannedEnd = productionBatch.getParams().get("endPlannedEnd");
        wrapper.between(beginPlannedEnd != null && endPlannedEnd != null, ProductionBatch::getPlannedEnd, beginPlannedEnd, endPlannedEnd);
        return wrapper;
    }

}
