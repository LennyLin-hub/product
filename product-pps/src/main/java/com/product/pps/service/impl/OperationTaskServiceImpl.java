package com.product.pps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.annotation.BizIdPrefix;
import com.product.common.constant.OperationTaskConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.core.result.AjaxResult;
import com.product.common.utils.StringUtils;
import com.product.common.utils.uuid.IdUtils;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.ProductionBatch;
import com.product.pps.dto.OperationTaskError;
import com.product.pps.mapper.OperationTaskMapper;
import com.product.pps.service.IOperationTaskService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工序任务Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-31
 */
@Service
public class OperationTaskServiceImpl extends ServiceImpl<OperationTaskMapper, OperationTask> implements IOperationTaskService {

    /**
     * 查询工序任务
     *
     * @param taskId 工序任务主键
     * @return 工序任务
     */
    @Override
    public OperationTask selectOperationTaskByTaskId(String taskId) {
        return getById(taskId);
    }

    /**
     * 查询工序任务列表
     *
     * @param operationTask 查询条件
     * @return 工序任务集合
     */
    @Override
    public List<OperationTask> selectOperationTaskList(OperationTask operationTask) {
        return list(buildQueryWrapper(operationTask));
    }

    /**
     * 分页查询工序任务列表
     *
     * @param page      分页参数
     * @param operationTask 查询条件
     * @return 分页结果
     */
    @Override
    public Page<OperationTask> selectOperationTaskPage(Page<OperationTask> page, OperationTask operationTask) {
        return this.page(page, buildQueryWrapper(operationTask));
    }

    /**
     * 新增工序任务
     *
     * @param operationTask 工序任务
     * @return 是否成功
     */
    @Override
    public boolean insertOperationTask(OperationTask operationTask) {
        if (StringUtils.isEmpty(operationTask.getTaskId())) {
            operationTask.setTaskId(buildBizId(operationTask));
        }
        if (StringUtils.isEmpty(operationTask.getStatus())) {
            operationTask.setStatus(StatusConstants.READY_OPERATION_TASK);
        }
        boolean saved = save(operationTask);
        return saved;
    }

    /**
     * 批量新增工序任务
     *
     * @param operationTasks 工序任务列表
     * @return 成功条数
     */
    @Override
    public int batchInsertOperationTask(List<OperationTask> operationTasks) {
        if (CollectionUtils.isEmpty(operationTasks)) {
            return 0;
        }
        operationTasks.forEach(item -> {
            if (StringUtils.isEmpty(item.getTaskId())) {
                item.setTaskId(buildBizId(item));
            }
        });
        boolean success = saveBatch(operationTasks);
        return success ? operationTasks.size() : 0;
    }

    /**
     * 修改工序任务
     *
     * @param operationTask 工序任务
     * @return 是否成功
     */
    @Override
    public boolean updateOperationTask(OperationTask operationTask) {
        boolean updated = updateById(operationTask);
        return updated;
    }

    /**
     * 批量删除工序任务
     *
     * @param taskIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteOperationTaskByTaskIds(String[] taskIds) {
        if (taskIds == null || taskIds.length == 0) {
            return false;
        }
        return removeByIds(Arrays.asList(taskIds));
    }

    /**
     * 删除工序任务信息
     *
     * @param taskId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteOperationTaskByTaskId(String taskId) {
        return removeById(taskId);
    }

    @Override
    public AjaxResult generateTask(List<String> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return AjaxResult.success("批次不能为空");
        }
        // 先校验批次状态
        List<ProductionBatch> productionBatches = Db.lambdaQuery(ProductionBatch.class)
                .select(ProductionBatch::getBatchId, ProductionBatch::getStatus)
                .in(ProductionBatch::getBatchId, batchIds)
                .list();
        List<OperationTaskError> errors = new ArrayList<>();
        List<OperationTask> operationTasks = new ArrayList<>();
        Map<String, ProductionBatch> batchMap = productionBatches.stream()
                .collect(Collectors.toMap(ProductionBatch::getBatchId, item -> item, (a, b) -> a));
        List<String> list = lambdaQuery()
                .select(OperationTask::getBatchId)
                .in(OperationTask::getBatchId, batchIds)
                .list()
                .stream()
                .map(OperationTask::getBatchId)
                .distinct()
                .collect(Collectors.toList());
        // 开始循环添加任务
        for (String batchId : batchIds) {
            ProductionBatch item = batchMap.get(batchId);
            if (item == null) {
                OperationTaskError error = new OperationTaskError();
                error.setBatchId(batchId);
                error.setErrorMessage("批次不存在");
                errors.add(error);
                continue;
            }
            if (!item.getStatus().equals(StatusConstants.RELEASED_PRODUCTION_BATCH)) {
                OperationTaskError error = new OperationTaskError();
                error.setBatchId(item.getBatchId());
                error.setErrorMessage("批次未释放");
                errors.add(error);
                continue;
            }
            if (list.contains(item.getBatchId())) {
                OperationTaskError error = new OperationTaskError();
                error.setBatchId(item.getBatchId());
                error.setErrorMessage("批次已存在任务");
                errors.add(error);
                continue;
            }
            LocalDateTime baseTime = LocalDateTime.now();
            long cumulativeMinutes = 0; // 累计时长（分钟）

            for (int i = 0; i < 3;) {
                OperationTask operationTask = new OperationTask();
                operationTask.setBatchId(item.getBatchId());
                operationTask.setStatus(StatusConstants.READY_OPERATION_TASK);
                operationTask.setOpCode(OperationTaskConstants.OP_CODE.get(i));
                operationTask.setStdDurationMin(OperationTaskConstants.STD_DURATION_MIM.get(i));
                // 最早开始时间 = 当前时间 + 前面所有工序的累计时长
                operationTask.setEarliestStart(baseTime.plus(cumulativeMinutes, ChronoUnit.MINUTES));
                operationTask.setSequence(Long.valueOf(++i));
                // 累加当前工序的时长，用于下一个工序的计算
                cumulativeMinutes += operationTask.getStdDurationMin();
                // 添加到列表
                operationTasks.add(operationTask);
            }
        }
        // 批量保存工序任务
        if (!operationTasks.isEmpty()) {
            saveBatch(operationTasks);
        }

        if (errors.isEmpty()) {
            AjaxResult result = AjaxResult.success("操作成功");
            result.put("errors", errors);
            return result;
        }
        if (!operationTasks.isEmpty()) {
            AjaxResult result = AjaxResult.success("操作完成，部分失败");
            result.put("errors", errors);
            return result;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        return AjaxResult.error("操作失败", data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult retryGenerateTask(String batchId) {
        List<OperationTask> tasks = lambdaQuery()
                .select(OperationTask::getStatus)
                .eq(OperationTask::getBatchId, batchId)
                .list();
        boolean hasNotAllowed = tasks.stream().anyMatch(item ->
                !StatusConstants.READY_OPERATION_TASK.equals(item.getStatus())
                        && !StatusConstants.SCHEDULED_OPERATION_TASK.equals(item.getStatus()));
        List<OperationTaskError> errors = new ArrayList<>();
        if (hasNotAllowed) {
            errors.add(new OperationTaskError(batchId, "仅允许READY或SCHEDULED状态的任务重新生成"));
            return AjaxResult.error("重新生成失败", errors);
        }
        lambdaUpdate().eq(OperationTask::getBatchId, batchId).remove();
        ProductionBatch productionBatch = Db.lambdaQuery(ProductionBatch.class)
                .select(ProductionBatch::getBatchId, ProductionBatch::getStatus)
                .eq(ProductionBatch::getBatchId, batchId)
                .last("limit 1").one();
        if (productionBatch == null) {
            errors.add(new OperationTaskError(batchId, "批次不存在"));
            return AjaxResult.error("重新生成失败", errors);
        }
        if (!productionBatch.getStatus().equals(StatusConstants.RELEASED_PRODUCTION_BATCH)) {
            errors.add(new OperationTaskError(batchId, "该批次当前状态不允许修改"));
            return AjaxResult.error("重新生成失败", errors);
        }
        LocalDateTime baseTime = LocalDateTime.now();
        long cumulativeMinutes = 0; // 累计时长（分钟）
        List<OperationTask> operationTasks = new ArrayList<>();
        for (int i = 0; i < 3;) {
            OperationTask operationTask = new OperationTask();
            operationTask.setBatchId(batchId);
            operationTask.setStatus(StatusConstants.READY_OPERATION_TASK);
            operationTask.setOpCode(OperationTaskConstants.OP_CODE.get(i));
            operationTask.setStdDurationMin(OperationTaskConstants.STD_DURATION_MIM.get(i));
            // 最早开始时间 = 当前时间 + 前面所有工序的累计时长
            operationTask.setEarliestStart(baseTime.plus(cumulativeMinutes, ChronoUnit.MINUTES));
            operationTask.setSequence(Long.valueOf(++i));
            // 累加当前工序的时长，用于下一个工序的计算
            cumulativeMinutes += operationTask.getStdDurationMin();
            // 添加到列表
            operationTasks.add(operationTask);
        }
        saveBatch(operationTasks);
        return AjaxResult.success();
    }

    @Override
    public boolean cancel(String taskId) {
        return lambdaUpdate().set(OperationTask::getStatus, StatusConstants.CANCELLED_OPERATION_TASK)
                .eq(OperationTask::getTaskId, taskId)
                .update();
    }

    @Override
    public boolean restore(String taskId) {
        return lambdaUpdate().set(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .eq(OperationTask::getTaskId, taskId)
                .update();
    }

    @Override
    public boolean revokeSchedule(String taskId) {
        // TODO 后续需要删除派工记录
        return lambdaUpdate().set(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .eq(OperationTask::getTaskId, taskId)
                .update();
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<OperationTask> buildQueryWrapper(OperationTask operationTask) {
        LambdaQueryWrapper<OperationTask> wrapper = new LambdaQueryWrapper<>();
        if (operationTask == null) {
            return wrapper;
        }
        wrapper.eq(operationTask.getBatchId() != null, OperationTask::getBatchId, operationTask.getBatchId());
        wrapper.eq(operationTask.getOpCode() != null, OperationTask::getOpCode, operationTask.getOpCode());
        wrapper.eq(operationTask.getSequence() != null, OperationTask::getSequence, operationTask.getSequence());
        wrapper.eq(operationTask.getStdDurationMin() != null, OperationTask::getStdDurationMin, operationTask.getStdDurationMin());
        wrapper.eq(operationTask.getEarliestStart() != null, OperationTask::getEarliestStart, operationTask.getEarliestStart());
        wrapper.eq(operationTask.getStatus() != null, OperationTask::getStatus, operationTask.getStatus());
        return wrapper;
    }


    private String buildBizId(Object entity) {
        BizIdPrefix annotation = entity.getClass().getAnnotation(BizIdPrefix.class);
        String prefix = annotation != null ? annotation.value() : null;
        String suffix = IdUtils.simpleUUID();
        return StringUtils.isNotEmpty(prefix) ? prefix + suffix : suffix;
    }
}
