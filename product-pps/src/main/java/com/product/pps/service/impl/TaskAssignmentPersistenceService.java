package com.product.pps.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.common.constant.StatusConstants;
import com.product.common.exception.ServiceException;
import com.product.domain.entity.TaskAssignment;
import com.product.pps.mapper.OperationTaskMapper;
import com.product.pps.mapper.TaskAssignmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 派工结果持久化服务。
 *
 * 说明：负责排程结果的防重检查、批量写入和任务状态回写。
 * 排程算法本身留在 {@link TaskSchedulingCalculator}，编排流程留在 {@link TaskAssignmentServiceImpl}。
 */
@Slf4j
@Service
public class TaskAssignmentPersistenceService extends ServiceImpl<TaskAssignmentMapper, TaskAssignment> {
    @Autowired
    private OperationTaskMapper operationTaskMapper;

    /**
     * 持久化单个批次的排程结果。
     */
    public boolean persistBatchResult(TaskSchedulingCalculator.ScheduleBatchResult batchResult, int batchSize) {
        // 查询哪些 task_id 已经存在派工记录
        if (batchResult == null || CollectionUtils.isEmpty(batchResult.getAssignments())) {
            return true;
        }

        List<String> existingTaskIds = baseMapper.selectExistingTaskIds(batchResult.getTaskIds());
        if (CollectionUtils.isNotEmpty(existingTaskIds)) {
            throw new ServiceException("任务已存在派工记录: " + String.join(",", existingTaskIds));
        }
        // 这里需要
        boolean saved = saveBatch(batchResult.getAssignments(), batchSize);
        if (!saved) {
            return false;
        }

        int updated = operationTaskMapper.batchMarkScheduled(
                batchResult.getTaskIds(),
                StatusConstants.READY_OPERATION_TASK,
                StatusConstants.SCHEDULED_OPERATION_TASK
        );
        if (updated != batchResult.getTaskIds().size()) {
            throw new ServiceException("任务状态更新失败，预期更新" + batchResult.getTaskIds().size() + "条，实际更新" + updated + "条");
        }
        return true;
    }

    /**
     * 持久化所有批次的排程结果。
     */
    public boolean persistAllBatchResults(List<TaskSchedulingCalculator.ScheduleBatchResult> batchResults, int batchSize) {
        if (CollectionUtils.isEmpty(batchResults)) {
            return true;
        }
        for (TaskSchedulingCalculator.ScheduleBatchResult batchResult : batchResults) {
            if (!persistBatchResult(batchResult, batchSize)) {
                return false;
            }
        }
        return true;
    }
}
