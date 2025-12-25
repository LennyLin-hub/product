package com.product.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.demand.mapper.CustomerOrderMapper;
import com.product.demand.service.ICustomerOrderService;
import com.product.domain.entity.CustomerOrder;
import com.product.domain.vo.CustomerOrderVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 订单Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-25
 */
@Service
public class CustomerOrderServiceImpl extends ServiceImpl<CustomerOrderMapper, CustomerOrder> implements ICustomerOrderService {

    @Autowired
    private CustomerOrderMapper customerOrderMapper;

    /**
     * 查询订单
     *
     * @param orderId 订单主键
     * @return 订单
     */
    @Override
    public CustomerOrder selectCustomerOrderByOrderId(String orderId) {
        return getById(orderId);
    }

    /**
     * 查询订单列表
     *
     * @param customerOrder 查询条件
     * @return 订单集合
     */
    @Override
    public List<CustomerOrder> selectCustomerOrderList(CustomerOrder customerOrder) {
        return list(buildQueryWrapper(customerOrder));
    }

    /**
     * 分页查询订单列表
     *
     * @param page      分页参数
     * @param customerOrder 查询条件
     * @return 分页结果
     */
    @Override
    public Page<CustomerOrderVO> selectCustomerOrderPage(Page<CustomerOrderVO> page, CustomerOrder customerOrder) {
        return customerOrderMapper.selectCustomerOrderPage(page, customerOrder);
    }

    /**
     * 新增订单
     *
     * @param customerOrder 订单
     * @return 是否成功
     */
    @Override
    public boolean insertCustomerOrder(CustomerOrder customerOrder) {
        boolean saved = save(customerOrder);
        return saved;
    }

    /**
     * 批量新增订单
     *
     * @param customerOrders 订单列表
     * @return 成功条数
     */
    @Override
    public int batchInsertCustomerOrder(List<CustomerOrder> customerOrders) {
        if (CollectionUtils.isEmpty(customerOrders)) {
            return 0;
        }
        boolean success = saveBatch(customerOrders);
        return success ? customerOrders.size() : 0;
    }

    /**
     * 修改订单
     *
     * @param customerOrder 订单
     * @return 是否成功
     */
    @Override
    public boolean updateCustomerOrder(CustomerOrder customerOrder) {
        boolean updated = updateById(customerOrder);
        return updated;
    }

    /**
     * 批量删除订单
     *
     * @param orderIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteCustomerOrderByOrderIds(String[] orderIds) {
        if (orderIds == null || orderIds.length == 0) {
            return false;
        }
        return removeByIds(Arrays.asList(orderIds));
    }

    /**
     * 删除订单信息
     *
     * @param orderId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteCustomerOrderByOrderId(String orderId) {
        return removeById(orderId);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<CustomerOrder> buildQueryWrapper(CustomerOrder customerOrder) {
        LambdaQueryWrapper<CustomerOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(customerOrder.getCustomerId() != null, CustomerOrder::getCustomerId, customerOrder.getCustomerId());
        wrapper.eq(customerOrder.getDueDate() != null, CustomerOrder::getDueDate, customerOrder.getDueDate());
        wrapper.eq(customerOrder.getPriority() != null, CustomerOrder::getPriority, customerOrder.getPriority());
        wrapper.eq(customerOrder.getStatus() != null, CustomerOrder::getStatus, customerOrder.getStatus());
        // BaseEntity 提供的是 createTime / updateTime；这里原来调用了不存在的 getCreatedTime / getUpdatedTime 导致无法编译
        wrapper.eq(customerOrder.getCreateTime() != null, CustomerOrder::getCreateTime, customerOrder.getCreateTime());
        wrapper.eq(customerOrder.getUpdateTime() != null, CustomerOrder::getUpdateTime, customerOrder.getUpdateTime());
        return wrapper;
    }

}
