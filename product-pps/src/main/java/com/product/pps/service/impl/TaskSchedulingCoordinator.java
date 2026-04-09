package com.product.pps.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.domain.dto.TaskAssignmentDTO;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
import com.product.pps.dto.ScheduleExecutionResult;
import com.product.pps.dto.ScheduleProgressDTO;
import com.product.pps.enums.SchedulePhase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 排程协调器。
 *
 * 只负责编排排程流程，不承载具体算法与持久化细节：
 * - 读取排程基础数据
 * - 分批计算任务分配
 * - 统一提交落库
 * - 回传进度与耗时统计
 */
@Slf4j
@Service
public class TaskSchedulingCoordinator {
    @Value("${product.pps.schedule.batch-size:200}")
    private int scheduleBatchSize;

    /**
     * 进度推送阈值，单位为任务数。
     *
     * 该配置与批次大小解耦，用于控制进度回调的频率：
     * - 值越小，推送越频繁
     * - 值越大，推送越稀疏
     * - 如果单批处理跨过多个阈值，会在当前批次结束后补发多个进度快照
     */
    @Value("${product.pps.schedule.progress-push-task-step:50}")
    private int progressPushTaskStep;

    @Autowired
    private TaskSchedulingQueryService taskSchedulingQueryService;
    @Autowired
    private TaskSchedulingCalculator taskSchedulingCalculator;
    @Autowired
    private TaskAssignmentPersistenceService taskAssignmentPersistenceService;
    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 执行全量排程计划。
     */
    public ScheduleExecutionResult executeSchedulePlan(TaskAssignmentDTO taskAssignmentDTO) {
        return executeSchedulePlan(taskAssignmentDTO, null);
    }

    /**
     * 执行全量排程计划，并在计算过程中回传进度快照。
     */
    public ScheduleExecutionResult executeSchedulePlan(TaskAssignmentDTO taskAssignmentDTO,
                                                       Consumer<ScheduleProgressDTO> progressConsumer) {
        return runScheduleAll(taskAssignmentDTO, progressConsumer);
    }

    /**
     * 排程核心编排：仅负责流程调度，计算委托 Calculator，持久化委托 PersistenceService。
     *
     * @param taskAssignmentDTO    排程参数（可指定排程开始时间）
     * @param progressConsumer     进度回调（可为 null）
     * @return 排程执行结果
     */
    private ScheduleExecutionResult runScheduleAll(TaskAssignmentDTO taskAssignmentDTO, Consumer<ScheduleProgressDTO> progressConsumer) {
        long totalStart = System.currentTimeMillis();

        // 阶段1: 准备 — 获取排程基准时间、可用机台、任务总数、日历、机台内存快照
        LocalDateTime assignmentStart = resolveAssignmentStart(taskAssignmentDTO);
        CompletableFuture<List<Resource>> machinesFuture = CompletableFuture.supplyAsync(
                taskSchedulingQueryService::loadAvailableMachines,
                threadPoolTaskExecutor);
        CompletableFuture<Integer> totalTaskCountFuture = CompletableFuture.supplyAsync(
                taskSchedulingQueryService::countReadyTasks,
                threadPoolTaskExecutor);

        List<Resource> machines = machinesFuture.join();
        if (CollectionUtils.isEmpty(machines)) {
            return ScheduleExecutionResult.failure("没有可用机台");
        }

        CompletableFuture<Map<Long, Calendar>> calendarMapFuture = machinesFuture.thenApplyAsync(
                taskSchedulingQueryService::loadCalendarMap,
                threadPoolTaskExecutor);

        int totalTaskCount = totalTaskCountFuture.join();
        if (totalTaskCount == 0) {
            notifyScheduleProgress(progressConsumer, 0, 0, 0, 100, SchedulePhase.NO_TASK);
            return ScheduleExecutionResult.success(0, 0);
        }
        notifyScheduleProgress(progressConsumer, totalTaskCount, 0, 0, 0, SchedulePhase.STARTED);

        Map<Long, Calendar> calendarMap = calendarMapFuture.join();
        TaskSchedulingCalculator.MachineRuntimeContext runtimeContext = taskSchedulingCalculator.buildMachineRuntimeContext(machines);

        // 阶段2: 分批计算（核心耗时），进度 0-90%
        List<TaskSchedulingCalculator.ScheduleBatchResult> batchResults = new ArrayList<>();
        long current = 1L;
        int processedTaskCount = 0;
        int lastProgressPushTaskCount = 0;
        long calculateStart = System.currentTimeMillis();

        while (true) {
            // 分批获取状态就绪的任务
            Page<OperationTask> taskPage = taskSchedulingQueryService.loadReadyTaskPage(current, scheduleBatchSize);
            List<OperationTask> tasks = taskPage.getRecords();
            if (CollectionUtils.isEmpty(tasks)) {
                break;
            }
            // 利用贪心算法匹配最早开始的机台
            // TODO: 这里应该让在前端请求侧传策略参数，可以选择最早开始、最低成本或者是最快结束，增加另外两种算法处理
            // TODO: 为机台以及模具添加能力矩阵，先预筛选除机台再进行贪心算法
            TaskSchedulingCalculator.ScheduleBatchResult batchResult = taskSchedulingCalculator.calculateBatchAssignments(
                    tasks, machines, calendarMap, runtimeContext, assignmentStart);
            batchResults.add(batchResult);
            processedTaskCount += tasks.size();

            // 按独立配置的任务阈值推送进度，避免与批次大小耦合
            lastProgressPushTaskCount = notifyScheduleProgressByTaskStep(progressConsumer,
                    totalTaskCount,
                    lastProgressPushTaskCount,
                    processedTaskCount,
                    batchResults.size());

            if (!taskPage.hasNext()) {
                break;
            }
            current++;
        }

        if (CollectionUtils.isEmpty(batchResults)) {
            return ScheduleExecutionResult.success(totalTaskCount, 0);
        }

        // 阶段3: 统一入库（事务内）
        long persistStart = System.currentTimeMillis();
        Boolean persisted = transactionTemplate.execute(status ->
                taskAssignmentPersistenceService.persistAllBatchResults(batchResults, scheduleBatchSize));

        // 阶段4: 统计耗时
        long totalCost = System.currentTimeMillis() - totalStart;
        long calculateCost = persistStart - calculateStart;
        long persistCost = System.currentTimeMillis() - persistStart;

        // 阶段5: 进度推送 100%（成功）或标记失败
        if (Boolean.TRUE.equals(persisted)) {
            log.info("scheduleAll completed: tasks={}, batches={}, machines={}, batchSize={}, calculateCostMs={}, persistCostMs={}, totalCostMs={}",
                    totalTaskCount, batchResults.size(), machines.size(), scheduleBatchSize, calculateCost, persistCost, totalCost);
            notifyScheduleProgress(progressConsumer, totalTaskCount, totalTaskCount, batchResults.size(), 100, SchedulePhase.SUCCESS);
            return ScheduleExecutionResult.success(totalTaskCount, batchResults.size());
        }

        log.warn("scheduleAll failed: tasks={}, batches={}, machines={}, batchSize={}, calculateCostMs={}, persistCostMs={}, totalCostMs={}",
                totalTaskCount, batchResults.size(), machines.size(), scheduleBatchSize, calculateCost, persistCost, totalCost);
        return ScheduleExecutionResult.failure("排程结果落库失败", totalTaskCount, batchResults.size());
    }

    /**
     * 解析排程开始时间。
     */
    private LocalDateTime resolveAssignmentStart(TaskAssignmentDTO taskAssignmentDTO) {
        if (taskAssignmentDTO != null && taskAssignmentDTO.getAssignmentStart() != null) {
            return taskAssignmentDTO.getAssignmentStart();
        }
        return LocalDateTime.now();
    }

    /**
     * 向调用方推送排程进度。
     *
     * 调用方通过 Consumer 决定进度如何处理：
     * - 同步调用（progressConsumer = null）：不推送，直接跳过
     * - 异步排程任务：写入 schedule_job 表，前端轮询获取实时进度
     */
    private void notifyScheduleProgress(Consumer<ScheduleProgressDTO> progressConsumer,
                                        int totalTaskCount,
                                        int processedTaskCount,
                                        int batchCount,
                                        int progressPercent,
                                        SchedulePhase phase) {
        // 同步调用时 consumer 为 null，无需推送进度
        if (progressConsumer == null) {
            return;
        }

        // 组装进度快照，通过回调交给调用方处理（如写入 DB 供前端轮询）
        ScheduleProgressDTO progress = new ScheduleProgressDTO();
        progress.setTotalTaskCount(totalTaskCount);
        progress.setProcessedTaskCount(processedTaskCount);
        progress.setBatchCount(batchCount);
        progress.setProgressPercent(progressPercent);
        progress.setPhase(phase.getCode());
        progressConsumer.accept(progress);
    }

    /**
     * 按任务数阈值推送计算阶段进度。
     *
     * 说明：
     * - 阈值由独立配置项控制，不依赖分页大小
     * - 如果一批任务跨过多个阈值，会连续补发多个进度快照
     *
     * @param progressConsumer      进度回调
     * @param totalTaskCount        总任务数
     * @param lastProgressPushCount 上一次已推送到的任务数
     * @param processedTaskCount    当前已处理任务数
     * @param batchCount            当前已完成批次数
     * @return 最新一次推送到的任务数
     */
    private int notifyScheduleProgressByTaskStep(Consumer<ScheduleProgressDTO> progressConsumer,
                                                  int totalTaskCount,
                                                  int lastProgressPushCount,
                                                  int processedTaskCount,
                                                  int batchCount) {
        // 同步调用或无任务时跳过
        if (progressConsumer == null || totalTaskCount <= 0) {
            return lastProgressPushCount;
        }

        // 按固定任务步长推送，解耦推送频率与批次大小
        // 例如 step=50、totalTask=1000 时，每处理 50 个任务推送一次（0%、5%、10%...90%）
        int step = Math.max(1, progressPushTaskStep);
        // 计算下一个应推送的任务数，确保至少推进一个 step
        int nextPushTaskCount = Math.max(step, lastProgressPushCount + step);
        int latestPushedTaskCount = lastProgressPushCount;

        // 一批任务可能跨越多个推送节点，循环补推所有落下的进度
        while (nextPushTaskCount <= processedTaskCount) {
            int progressPercent = Math.min(90, (int) Math.round(nextPushTaskCount * 90.0 / totalTaskCount));
            notifyScheduleProgress(progressConsumer,
                    totalTaskCount,
                    nextPushTaskCount,
                    batchCount,
                    progressPercent,
                    SchedulePhase.CALCULATING);
            latestPushedTaskCount = nextPushTaskCount;
            nextPushTaskCount += step;
        }
        // 返回最新推送位置，供下次调用时判断是否需要继续推送
        return latestPushedTaskCount;
    }
}
