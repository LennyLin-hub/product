package com.product.pps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.OperationTask;

/**
 * 工序任务Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2025-12-31
 */
@Mapper
public interface OperationTaskMapper extends BaseMapper<OperationTask> {
}
