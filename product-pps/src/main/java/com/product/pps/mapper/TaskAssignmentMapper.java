package com.product.pps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.TaskAssignment;

/**
 * 派工/排程结果Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2026-01-02
 */
@Mapper
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {
}
