package com.product.execute.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.TaskEvent;

/**
 * 任务事件日志（全流程追溯核心）Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2026-01-03
 */
@Mapper
public interface TaskEventMapper extends BaseMapper<TaskEvent> {
}
