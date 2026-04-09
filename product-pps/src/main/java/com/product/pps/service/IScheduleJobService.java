package com.product.pps.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.domain.dto.TaskAssignmentDTO;
import com.product.domain.entity.ScheduleJob;

/**
 * 排程任务记录Service接口
 */
public interface IScheduleJobService {
    String scheduleAllAsync(TaskAssignmentDTO taskAssignmentDTO);

    ScheduleJob selectScheduleJobByJobId(String jobId);

    Page<ScheduleJob> selectScheduleJobPage(Page<ScheduleJob> page, ScheduleJob scheduleJob);
}
