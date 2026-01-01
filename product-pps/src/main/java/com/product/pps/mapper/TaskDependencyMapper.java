package com.product.pps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.product.domain.entity.TaskDependency;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务依赖 Mapper
 */
@Mapper
public interface TaskDependencyMapper extends BaseMapper<TaskDependency> {
}
