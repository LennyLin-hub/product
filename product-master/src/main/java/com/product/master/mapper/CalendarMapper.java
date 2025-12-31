package com.product.master.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.product.domain.entity.Calendar;

/**
 * 班次/日历Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2026-01-01
 */
@Mapper
public interface CalendarMapper extends BaseMapper<Calendar> {
}
