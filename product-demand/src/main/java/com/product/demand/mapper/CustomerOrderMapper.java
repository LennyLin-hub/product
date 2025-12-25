package com.product.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.domain.vo.CustomerOrderVO;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.CustomerOrder;

/**
 * 订单Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2025-12-25
 */
@Mapper
public interface CustomerOrderMapper extends BaseMapper<CustomerOrder> {
    Page<CustomerOrderVO> selectCustomerOrderPage(IPage<CustomerOrderVO> page, CustomerOrder customerOrder);
}
