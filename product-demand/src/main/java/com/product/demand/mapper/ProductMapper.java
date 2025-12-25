package com.product.demand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.Product;

/**
 * 产品Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2025-12-23
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
