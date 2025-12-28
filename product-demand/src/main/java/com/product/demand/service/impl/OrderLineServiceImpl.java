package com.product.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.common.annotation.BizIdPrefix;
import com.product.common.utils.StringUtils;
import com.product.common.utils.uuid.IdUtils;
import com.product.demand.mapper.OrderLineMapper;
import com.product.demand.service.IOrderLineService;
import com.product.domain.entity.OrderLine;
import com.product.domain.entity.ProductionBatch;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
/**
 * 订单明细Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-27
 */
@Service
public class OrderLineServiceImpl extends ServiceImpl<OrderLineMapper, OrderLine> implements IOrderLineService {

    /**
     * 查询订单明细
     *
     * @param orderLineId 订单明细主键
     * @return 订单明细
     */
    @Override
    public OrderLine selectOrderLineByOrderLineId(String orderLineId) {
        OrderLine orderLine = getById(orderLineId);
        if (orderLine == null) {
            return null;
        }
        List<ProductionBatch> productionBatchList = baseMapper.selectProductionBatchList(orderLineId);
        orderLine.setProductionBatchList(productionBatchList);
        return orderLine;
    }

    /**
     * 查询订单明细列表
     *
     * @param orderLine 查询条件
     * @return 订单明细集合
     */
    @Override
    public List<OrderLine> selectOrderLineList(OrderLine orderLine) {
        return list(buildQueryWrapper(orderLine));
    }

    /**
     * 分页查询订单明细列表
     *
     * @param page      分页参数
     * @param orderLine 查询条件
     * @return 分页结果
     */
    @Override
    public Page<OrderLine> selectOrderLinePage(Page<OrderLine> page, OrderLine orderLine) {
        return this.page(page, buildQueryWrapper(orderLine));
    }

    /**
     * 新增订单明细
     *
     * @param orderLine 订单明细
     * @return 是否成功
     */
    @Override
    public boolean insertOrderLine(OrderLine orderLine) {
        if (StringUtils.isEmpty(orderLine.getOrderId())) {
            orderLine.setOrderId(buildBizId(orderLine));
        }
        boolean saved = save(orderLine);
        if (saved) {
            saveOrUpdateProductionBatch(orderLine);
        }
        return saved;
    }

    private String buildBizId(Object entity) {
        BizIdPrefix annotation = entity.getClass().getAnnotation(BizIdPrefix.class);
        String prefix = annotation != null ? annotation.value() : null;
        String suffix = IdUtils.simpleUUID();
        return StringUtils.isNotEmpty(prefix) ? prefix + suffix : suffix;
    }

    /**
     * 批量新增订单明细
     *
     * @param orderLines 订单明细列表
     * @return 成功条数
     */
    @Override
    public int batchInsertOrderLine(List<OrderLine> orderLines) {
        if (CollectionUtils.isEmpty(orderLines)) {
            return 0;
        }
        orderLines.forEach(orderLine -> {
            if (StringUtils.isEmpty(orderLine.getOrderId())) {
                orderLine.setOrderId(buildBizId(orderLine));
            }
        });
        boolean success = saveBatch(orderLines);
        return success ? orderLines.size() : 0;
    }

    /**
     * 修改订单明细
     *
     * @param orderLine 订单明细
     * @return 是否成功
     */
    @Override
    public boolean updateOrderLine(OrderLine orderLine) {
        boolean updated = updateById(orderLine);
        if (updated) {
            baseMapper.deleteProductionBatchByBatchId(orderLine.getOrderLineId());
            saveOrUpdateProductionBatch(orderLine);
        }
        return updated;
    }

    /**
     * 批量删除订单明细
     *
     * @param orderLineIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteOrderLineByOrderLineIds(String[] orderLineIds) {
        if (orderLineIds == null || orderLineIds.length == 0) {
            return false;
        }
        baseMapper.deleteProductionBatchByBatchIds(orderLineIds);
        return removeByIds(Arrays.asList(orderLineIds));
    }

    /**
     * 删除订单明细信息
     *
     * @param orderLineId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteOrderLineByOrderLineId(Long orderLineId) {
        baseMapper.deleteProductionBatchByBatchId(orderLineId);
        return removeById(orderLineId);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<OrderLine> buildQueryWrapper(OrderLine orderLine) {
        LambdaQueryWrapper<OrderLine> wrapper = new LambdaQueryWrapper<>();
        if (orderLine == null) {
            return wrapper;
        }
        wrapper.eq(orderLine.getOrderId() != null, OrderLine::getOrderId, orderLine.getOrderId());
        wrapper.eq(orderLine.getProductId() != null, OrderLine::getProductId, orderLine.getProductId());
        wrapper.eq(orderLine.getQty() != null, OrderLine::getQty, orderLine.getQty());
        wrapper.eq(orderLine.getStatus() != null, OrderLine::getStatus, orderLine.getStatus());
        return wrapper;
    }

    /**
     * 处理子表数据
     */
    private void saveOrUpdateProductionBatch(OrderLine orderLine) {
        List<ProductionBatch> productionBatchList = orderLine.getProductionBatchList();
        Long orderLineId = orderLine.getOrderLineId();
        if (CollectionUtils.isEmpty(productionBatchList)) {
            return;
        }
        productionBatchList.forEach(item -> {
            item.setOrderLineId(orderLineId);
        });
        baseMapper.batchProductionBatch(productionBatchList);
    }
}
