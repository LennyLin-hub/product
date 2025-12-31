package com.product.pps.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单行数量预占用 Mapper
 *
 * @author product
 * @date 2025-12-29
 */
@Mapper
public interface OrderLineAllocationMapper {
    /**
     * 原子预占用：仅当 allocated_qty + batchQty <= qty 时更新成功
     *
     * @param orderLineId 订单行ID
     * @param batchQty    预占数量
     * @return 影响行数
     */
    int allocateQty(@Param("orderLineId") Long orderLineId, @Param("batchQty") Long batchQty);

    /**
     * 释放预占用：直接减少 allocated_qty（batchQty 为负数）
     *
     * @param orderLineId 订单行ID
     * @param batchQty    释放数量（负数）
     * @return 影响行数
     */
    int releaseQty(@Param("orderLineId") Long orderLineId, @Param("batchQty") Long batchQty);
}
