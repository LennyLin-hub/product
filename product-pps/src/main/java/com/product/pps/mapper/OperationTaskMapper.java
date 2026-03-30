package com.product.pps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.product.domain.entity.OperationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工序任务Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2025-12-31
 */
@Mapper
public interface OperationTaskMapper extends BaseMapper<OperationTask> {
    int batchMarkScheduled(@Param("taskIds") List<String> taskIds,
                           @Param("fromStatus") String fromStatus,
                           @Param("toStatus") String toStatus);
}
