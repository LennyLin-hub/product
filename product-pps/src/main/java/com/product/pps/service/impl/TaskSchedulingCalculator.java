package com.product.pps.service.impl;

import com.product.common.constant.StatusConstants;
import com.product.common.exception.ServiceException;
import com.product.common.utils.StringUtils;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
import com.product.domain.entity.TaskAssignment;
import com.product.pps.dto.MachineRuntimeStatsDTO;
import com.product.pps.mapper.TaskAssignmentMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排程计算器。
 *
 * 说明：这里只保留纯排程计算逻辑和机台运行时快照构建，
 * 让 TaskAssignmentServiceImpl 只负责编排、事务和持久化。
 */
@Component
public class TaskSchedulingCalculator {
    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;

    /**
     * 计算一批任务的机台分配方案（核心排程算法）
     *
     * 算法策略：贪心算法
     * - 按任务列表顺序依次处理
     * - 每个任务选择当前最早可用的机台
     * - 选择后立即更新内存快照（确保后续任务基于最新状态）
     *
     * 执行流程：
     * 1. 遍历每个任务
     * 2. 为每个任务调用 chooseMachine() 选择最优机台
     * 3. 创建 TaskAssignment 派工记录
     * 4. 更新内存中的机台可用时间和序号
     * 5. 返回批次的排程结果
     *
     * 数据流转：
     * 输入：OperationTask 列表（待排程任务）
     *  ↓
     * 计算：chooseMachine() 选择机台 + 时间窗口
     *  ↓
     * 输出：TaskAssignment 列表（派工记录）
     *
     * 内存快照更新机制：
     * 初始状态：M001(10:00, 序号1), M002(08:00, 序号1)
     * 任务1分配：选择M002(08:00-10:00), 更新 M002(10:00, 序号2)
     * 任务2分配：选择M001(10:00-12:00), 更新 M001(12:00, 序号2)
     * 任务3分配：选择M002(10:00-12:00), 更新 M002(12:00, 序号3)
     *
     * 关键特性：
     * - 纯内存计算：不涉及数据库操作
     * - 批次原子性：一个批次内的任务要么全部成功，要么全部失败
     * - 状态累积：runtimeContext 在批次内不断更新，影响后续任务
     * - 异常处理：任一任务无可用机台则抛出异常，整批失败
     *
     * @param tasks             待排程的任务列表（按优先级排序）
     * @param machines          可用机台列表
     * @param calendarMap       机台ID → 日历映射（包含班次、工作日规则）
     * @param runtimeContext    机台运行时上下文（内存快照，会被修改）
     * @param assignmentStart   排程开始时间基准（用户指定或当前时间）
     * @return 批次排程结果（包含派工记录列表和任务ID列表）
     * @throws ServiceException 如果某个任务没有可用机台
     */
    public ScheduleBatchResult calculateBatchAssignments(List<OperationTask> tasks,
                                                         List<Resource> machines,
                                                         Map<Long, Calendar> calendarMap,
                                                         MachineRuntimeContext runtimeContext,
                                                         LocalDateTime assignmentStart) {
        // 存储派工记录（待入库）
        List<TaskAssignment> assignments = new ArrayList<>(tasks.size());
        // 存储任务ID（用于后续批量更新任务状态）
        List<String> taskIds = new ArrayList<>(tasks.size());

        // 遍历每个任务，为其分配最优机台
        for (OperationTask task : tasks) {
            if (task == null) {
                continue;
            }

            // ===== 为当前任务选择最优机台 =====
            // 遍历所有可用机台，计算每台机的最早可用时间
            // 结合日历调整班次时间，处理跨班次顺延
            // 返回最早能开始任务的机台
            MachineChoice choice = chooseMachine(task, machines, calendarMap, assignmentStart, runtimeContext);
            if (choice == null) {
                throw new ServiceException("任务" + task.getTaskId() + "没有可用机台");
            }

            // ===== 创建派工记录 =====
            TaskAssignment assignment = new TaskAssignment();
            assignment.setTaskId(task.getTaskId());
            assignment.setMachineId(choice.machineId);
            assignment.setPlannedStart(choice.plannedStart);
            assignment.setPlannedEnd(choice.plannedEnd);
            assignment.setSequenceOnResource(choice.sequenceOnResource);
            assignments.add(assignment);
            taskIds.add(task.getTaskId());

            // ===== 更新内存快照（关键！）=====
            // 当前批次内不回写数据库，直接推进内存态的机台可用时间与序号
            // 下一个任务将基于新的快照进行计算
            // 例如：任务1占用 M001(08:00-10:00)后，M001 的最早可用时间更新为 10:00
            runtimeContext.update(choice.machineId, choice.plannedEnd, choice.sequenceOnResource);
        }
        return new ScheduleBatchResult(assignments, taskIds);
    }

    /**
     * 构建机台运行时上下文（内存快照）
     *
     * 核心作用：预加载机台的运行状态到内存，避免排程过程中频繁查询数据库
     *
     * 设计目的：
     * 1. 性能优化：将多次数据库查询减少为一次批量查询
     * 2. 内存计算：在批次计算中不断更新内存状态，避免每次都查库
     * 3. 并发安全：读取快照后，排程计算在内存中进行，不影响数据库
     *
     * 数据结构：
     * - MachineRuntimeContext.nextAvailableTimeMap: 机台ID → 最早可用时间
     * - MachineRuntimeContext.nextSequenceMap: 机台ID → 下一个序号
     *
     * 加载的数据来源（一次性查询）：
     * 1. 最近任务结束时间：每台机 SCHEDULED/RUNNING/PAUSED 状态任务的最大 planned_end
     * 2. 当前最大序号：每台机 task_assignment 表中最大的 sequence_on_resource
     *
     * 数据流转：
     * 初始状态（数据库预加载） → 批次计算中（内存更新） → 最终结果（批量入库）
     *
     * 使用示例：
     * <pre>{@code
     * // 1. 排程开始前，构建内存快照
     * MachineRuntimeContext context = taskSchedulingCalculator.buildMachineRuntimeContext(machines);
     * // 此时：
     * // - context.nextAvailableTimeMap.get("M001") = 2026-03-31 10:00（最近任务结束时间）
     * // - context.nextSequenceMap.get("M001") = 5（下一个序号）
     *
     * // 2. 批次计算中，更新内存状态（不回写数据库）
     * for (每个任务) {
     *     MachineChoice choice = chooseMachine(..., context);
     *     // 选择机台 M001，时间窗口 10:00-12:00，序号 5
     *     context.update("M001", 12:00, 5);
     *     // 此时：
     *     // - context.nextAvailableTimeMap.get("M001") = 12:00（更新了）
     *     // - context.nextSequenceMap.get("M001") = 6（下一个任务用序号6）
     * }
     *
     * // 3. 所有批次计算完成后，统一入库
     * transactionTemplate.execute(() -> persistAllBatchResults(batchResults));
     * }</pre>
     *
     * @param machines 机台列表
     * @return 机台运行时上下文（包含每台机的最早可用时间和下一个序号）
     */
    public MachineRuntimeContext buildMachineRuntimeContext(List<Resource> machines) {
        // 状态机器ID
        List<String> machineIds = machines.stream()
                .map(Resource::getResourceId)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        MachineRuntimeContext context = new MachineRuntimeContext();
        if (CollectionUtils.isEmpty(machineIds)) {
            return context;
        }
        /**
         * MachineRuntimeStatsDTO {
         *  private String machineId;
         *  private LocalDateTime latestEndTime;
         *  private Long maxSequence;
         * }
         */
        List<MachineRuntimeStatsDTO> runtimeStats = taskAssignmentMapper.selectMachineRuntimeStats(
                machineIds,
                List.of(
                        StatusConstants.SCHEDULED_OPERATION_TASK,
                        StatusConstants.RUNNING_OPERATION_TASK,
                        StatusConstants.PAUSED_OPERATION_TASK
                )
        );
        for (MachineRuntimeStatsDTO row : runtimeStats) {
            String machineId = row.getMachineId();
            LocalDateTime latestEndTime = row.getLatestEndTime();
            Long maxSequence = row.getMaxSequence();
            if (StringUtils.isNotEmpty(machineId)) {
                if (latestEndTime != null) {
                    context.nextAvailableTimeMap.put(machineId, latestEndTime);
                }
                if (maxSequence != null) {
                    context.nextSequenceMap.put(machineId, maxSequence + 1L);
                }
            }
        }
        return context;
    }

    /**
     * 为单个任务选择最优机台
     *
     * 选择策略：贪心算法（选择最早能开始任务的机台）
     *
     * 选择标准（优先级从高到低）：
     * 1. plannedStart 更早者优先
     * 2. plannedStart 相同时，plannedEnd 更早者优先
     *
     * 执行流程：
     * 1. 遍历所有可用机台
     * 2. 为每台机计算可执行的时间窗口：
     *    - 获取机台最早可用时间（从内存快照读取）
     *    - 计算候选时间 = max(任务最早开始时间, 机台可用时间, 排程开始时间)
     *    - 调整到班次开始时间（处理非工作时段）
     *    - 处理班次结束时间（跨班次则顺延到下一工作日）
     * 3. 比较所有机台的时间窗口，选择最优的
     *
     * 示例场景：
     * <pre>{@code
     * 任务：TASK-001, 标准时长 120分钟
     * 机台M001：可用时间 08:00
     * 机台M002：可用时间 09:00
     *
     * 计算过程：
     * M001: 08:00 + 120分钟 = 10:00
     * M002: 09:00 + 120分钟 = 11:00
     *
     * 选择结果：M001（开始时间更早）
     * }</pre>
     *
     * 班次调整示例：
     * <pre>{@code
     * 机台M001班次：08:00-17:00
     * 任务时长：240分钟（4小时）
     *
     * 情况1：候选时间 07:00（班次前）
     * → 调整到班次开始：08:00
     * → 执行时间：08:00-12:00 ✓
     *
     * 情况2：候选时间 16:00（班次中）
     * → 不需要调整：16:00
     * → 计算结束：16:00 + 4小时 = 20:00
     * → 跨越班次结束（17:00）
     * → 顺延到下一工作日：次日 08:00-12:00
     *
     * 情况3：候选时间 18:00（班次后）
     * → 顺延到下一工作日班次开始：次日 08:00
     * → 执行时间：次日 08:00-12:00
     * }</pre>
     *
     * @param task              待排程的任务
     * @param machines          可用机台列表
     * @param calendarMap       机台ID → 日历映射
     * @param assignmentStart   排程开始时间基准
     * @param runtimeContext    机台运行时上下文（内存快照）
     * @return 最优机台选择结果（如果所有机台都不可用则返回null）
     */
    private MachineChoice chooseMachine(OperationTask task,
                                        List<Resource> machines,
                                        Map<Long, Calendar> calendarMap,
                                        LocalDateTime assignmentStart,
                                        MachineRuntimeContext runtimeContext) {
        MachineChoice best = null;
        // 遍历所有可用机台，计算每台机的时间窗口
        for (Resource machine : machines) {
            if (machine == null) {
                continue;
            }

            // 获取机台关联的日历（包含班次、工作日规则）
            Calendar calendar = calendarMap.get(machine.getCalendarId());

            // 获取该机台的最早可用时间（从内存快照读取）
            LocalDateTime machineNextTime = runtimeContext.getNextAvailableTime(machine.getResourceId());

            // 计算候选时间 = 取三个时间中的最大值
            // 1) 任务最早开始时间（业务约束）
            // 2) 机台可用时间（前序任务结束时间）
            // 3) 排程开始时间基准（用户指定或当前时间）
            LocalDateTime candidate = maxTime(task.getEarliestStart(), machineNextTime, assignmentStart);

            // 调整到班次开始时间
            // 如果候选时间在非工作时段，顺延到下一个工作日的班次开始时间
            LocalDateTime plannedStart = adjustToShiftStart(calendar, candidate);
            if (plannedStart == null) {
                continue;  // 无法调整到有效时间，跳过该机台
            }

            // 获取任务标准时长（分钟）
            long duration = task.getStdDurationMin() == null ? 0L : task.getStdDurationMin();

            // 计算实际执行时间窗口
            // 判断任务是否会跨越下班时间，如果是则整体顺延到下一工作日班次开始
            TimeWindow window = adjustForShiftEnd(calendar, plannedStart, duration);
            if (window == null) {
                continue;  // 无法在有效工作日完成，跳过该机台
            }

            // 获取该机台上的下一个序号（从内存快照读取）
            Long sequenceOnResource = runtimeContext.getNextSequence(machine.getResourceId());

            // 创建该机台的选择方案
            MachineChoice choice = new MachineChoice(machine.getResourceId(), window.start, window.end, sequenceOnResource);

            // 比较并选择最优方案
            // 优先级：1)开始时间更早  2)开始时间相同时结束时间更早
            if (best == null
                    || choice.plannedStart.isBefore(best.plannedStart)
                    || (choice.plannedStart.isEqual(best.plannedStart) && choice.plannedEnd.isBefore(best.plannedEnd))) {
                best = choice;
            }
        }
        return best;  // 返回最早开始的机台
    }

    private LocalDateTime maxTime(LocalDateTime... times) {
        LocalDateTime max = null;
        if (times == null || times.length == 0) {
            return null;
        }
        for (LocalDateTime time : times) {
            if (time == null) {
                continue;
            }
            if (max == null || time.isAfter(max)) {
                max = time;
            }
        }
        return max;
    }

    private LocalDateTime adjustToShiftStart(Calendar calendar, LocalDateTime time) {
        if (time == null || calendar == null) {
            return time;
        }
        String shiftStart = calendar.getShiftStart();
        String shiftEnd = calendar.getShiftEnd();
        if (StringUtils.isEmpty(shiftStart) || StringUtils.isEmpty(shiftEnd)) {
            return time;
        }
        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(shiftStart);
            endTime = LocalTime.parse(shiftEnd);
        } catch (Exception ex) {
            return time;
        }
        LocalDate date = time.toLocalDate();
        date = nextWorkday(calendar, date);

        LocalDateTime shiftStartTime = LocalDateTime.of(date, startTime);
        LocalDateTime shiftEndTime = LocalDateTime.of(date, endTime);

        if (time.isBefore(shiftStartTime)) {
            return shiftStartTime;
        }
        if (!time.isBefore(shiftEndTime)) {
            LocalDate nextDate = nextWorkday(calendar, date.plusDays(1));
            return LocalDateTime.of(nextDate, startTime);
        }
        return time;
    }

    private TimeWindow adjustForShiftEnd(Calendar calendar, LocalDateTime start, long durationMinutes) {
        if (start == null) {
            return null;
        }
        LocalDateTime end = start.plusMinutes(durationMinutes);
        if (calendar == null) {
            return new TimeWindow(start, end);
        }
        String shiftStart = calendar.getShiftStart();
        String shiftEnd = calendar.getShiftEnd();
        if (StringUtils.isEmpty(shiftStart) || StringUtils.isEmpty(shiftEnd)) {
            return new TimeWindow(start, end);
        }
        LocalTime endTime;
        LocalTime startTime;
        try {
            startTime = LocalTime.parse(shiftStart);
            endTime = LocalTime.parse(shiftEnd);
        } catch (Exception ex) {
            return new TimeWindow(start, end);
        }
        LocalDate date = start.toLocalDate();
        LocalDateTime shiftEndTime = LocalDateTime.of(date, endTime);
        if (end.isAfter(shiftEndTime)) {
            LocalDate nextDate = nextWorkday(calendar, date.plusDays(1));
            LocalDateTime nextStart = LocalDateTime.of(nextDate, startTime);
            LocalDateTime nextEnd = nextStart.plusMinutes(durationMinutes);
            return new TimeWindow(nextStart, nextEnd);
        }
        return new TimeWindow(start, end);
    }

    private LocalDate nextWorkday(Calendar calendar, LocalDate date) {
        if (calendar == null || date == null) {
            return date;
        }
        String workdayPattern = calendar.getWorkdayPattern();
        LocalDate next = date;
        int guard = 0;
        while (!isWorkday(workdayPattern, next.getDayOfWeek()) && guard < 7) {
            next = next.plusDays(1);
            guard++;
        }
        return next;
    }

    private boolean isWorkday(String workdayPattern, DayOfWeek dayOfWeek) {
        if (StringUtils.isEmpty(workdayPattern) || dayOfWeek == null) {
            return true;
        }
        String normalized = workdayPattern.replace(" ", "");
        if (normalized.contains(",")) {
            String[] tokens = normalized.split(",");
            for (String token : tokens) {
                if (matchesDayToken(token, dayOfWeek)) {
                    return true;
                }
            }
            return false;
        }

        if (normalized.contains("-")) {
            String[] range = normalized.split("-");
            if (range.length != 2) {
                return false;
            }
            DayOfWeek start = parseDayOfWeek(range[0]);
            DayOfWeek end = parseDayOfWeek(range[1]);
            if (start == null || end == null) {
                return false;
            }
            int startValue = start.getValue();
            int endValue = end.getValue();
            int dayValue = dayOfWeek.getValue();
            if (startValue <= endValue) {
                return dayValue >= startValue && dayValue <= endValue;
            }
            return dayValue >= startValue || dayValue <= endValue;
        }

        return matchesDayToken(normalized, dayOfWeek);
    }

    private boolean matchesDayToken(String token, DayOfWeek dayOfWeek) {
        DayOfWeek parsed = parseDayOfWeek(token);
        return parsed != null && parsed.equals(dayOfWeek);
    }

    private DayOfWeek parseDayOfWeek(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        String key = token.trim().toLowerCase();
        switch (key) {
            case "mon":
            case "monday":
            case "1":
                return DayOfWeek.MONDAY;
            case "tue":
            case "tues":
            case "tuesday":
            case "2":
                return DayOfWeek.TUESDAY;
            case "wed":
            case "wednesday":
            case "3":
                return DayOfWeek.WEDNESDAY;
            case "thu":
            case "thur":
            case "thurs":
            case "thursday":
            case "4":
                return DayOfWeek.THURSDAY;
            case "fri":
            case "friday":
            case "5":
                return DayOfWeek.FRIDAY;
            case "sat":
            case "saturday":
            case "6":
                return DayOfWeek.SATURDAY;
            case "sun":
            case "sunday":
            case "7":
                return DayOfWeek.SUNDAY;
            default:
                return null;
        }
    }

    /**
     * 机台选择结果
     *
     * 职责：表示某个任务在某台机台上的执行方案
     *
     * 包含信息：
     * - machineId: 选中的机台ID
     * - plannedStart: 计划开始时间（已调整到班次开始）
     * - plannedEnd: 计划结束时间（已处理跨班次顺延）
     * - sequenceOnResource: 在该机台上的执行序号
     *
     * 使用场景：
     * - chooseMachine() 方法为每台机计算时间窗口后，创建 MachineChoice 对象
     * - 比较所有机台的 MachineChoice，选择最优的（开始时间最早的）
     * - 将选中的 MachineChoice 转换为 TaskAssignment 派工记录
     *
     * 示例：
     * <pre>{@code
     * // 为任务 TASK-001 选择机台
     * MachineChoice choice = new MachineChoice(
     *     "M001",                    // 机台ID
     *     LocalDateTime.of(...),    // 计划开始：2026-03-31 08:00
     *     LocalDateTime.of(...),    // 计划结束：2026-03-31 10:00
     *     5L                        // 序号：该机台上的第5个任务
     * );
     *
     * // 转换为派工记录
     * TaskAssignment assignment = new TaskAssignment();
     * assignment.setMachineId(choice.machineId);
     * assignment.setPlannedStart(choice.plannedStart);
     * assignment.setPlannedEnd(choice.plannedEnd);
     * assignment.setSequenceOnResource(choice.sequenceOnResource);
     * }</pre>
     */
    public static class MachineChoice {
        /** 机台ID */
        private final String machineId;
        /** 计划开始时间（已调整到班次开始，处理了非工作时段） */
        private final LocalDateTime plannedStart;
        /** 计划结束时间（已处理跨班次顺延） */
        private final LocalDateTime plannedEnd;
        /** 在该机台上的执行序号 */
        private final Long sequenceOnResource;

        /**
         * 构造机台选择结果
         *
         * @param machineId           机台ID
         * @param plannedStart        计划开始时间
         * @param plannedEnd          计划结束时间
         * @param sequenceOnResource  在该机台上的序号
         */
        public MachineChoice(String machineId, LocalDateTime plannedStart, LocalDateTime plannedEnd, Long sequenceOnResource) {
            this.machineId = machineId;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
            this.sequenceOnResource = sequenceOnResource;
        }
    }

    /**
     * 时间窗口
     *
     * 职责：表示任务的执行时间段（开始时间和结束时间）
     *
     * 使用场景：
     * - adjustForShiftEnd() 方法处理班次结束后，返回调整后的时间窗口
     * - 如果任务会在班次结束时中断，则将整个任务顺延到下一工作日班次开始
     *
     * 示例：
     * <pre>{@code
     * 场景1：任务在班次内完成
     * 机台班次：08:00-17:00
     * 任务时长：120分钟（2小时）
     * 候选开始：16:00
     * → TimeWindow(16:00, 18:00)
     * → 18:00 > 17:00，跨越班次结束
     * → 顺延到次日：TimeWindow(次日08:00, 次日10:00)
     *
     * 场景2：任务在班次内完成
     * 机台班次：08:00-17:00
     * 任务时长：60分钟（1小时）
     * 候选开始：14:00
     * → TimeWindow(14:00, 15:00)
     * → 15:00 < 17:00，在班次内完成
     * → 不需要调整
     * }</pre>
     */
    public static class TimeWindow {
        /** 开始时间 */
        private final LocalDateTime start;
        /** 结束时间 */
        private final LocalDateTime end;

        /**
         * 构造时间窗口
         *
         * @param start 开始时间
         * @param end   结束时间
         */
        public TimeWindow(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * 批次排程结果
     *
     * 职责：封装一个批次任务的排程计算结果
     *
     * 包含内容：
     * - assignments: 派工记录列表（待批量插入数据库）
     * - taskIds: 任务ID列表（用于批量更新任务状态为 SCHEDULED）
     *
     * 数据流转：
     * calculateBatchAssignments() 返回 ScheduleBatchResult
     *   ↓
     * 添加到 batchResults 列表
     *   ↓
     * 所有批次计算完成后
     *   ↓
     * persistAllBatchResults(batchResults) 批量入库
     *
     * 使用示例：
     * <pre>{@code
     * // 计算一个批次
     * ScheduleBatchResult batchResult = calculateBatchAssignments(tasks, machines, ...);
     *
     * // 批次结果包含：
     * // - 200 个 TaskAssignment 对象（派工记录）
     * // - 200 个任务ID（用于更新状态）
     *
     * // 后续处理：
     * // 1. 批量插入派工记录
     * saveBatch(batchResult.getAssignments());
     * // 2. 批量更新任务状态
     * batchMarkScheduled(batchResult.getTaskIds(), "READY", "SCHEDULED");
     * }</pre>
     */
    public static class ScheduleBatchResult {
        /** 派工记录列表（一个任务对应一条记录） */
        private final List<TaskAssignment> assignments;
        /** 任务ID列表（用于后续批量更新任务状态） */
        private final List<String> taskIds;

        /**
         * 构造批次排程结果
         *
         * @param assignments 派工记录列表
         * @param taskIds     任务ID列表
         */
        public ScheduleBatchResult(List<TaskAssignment> assignments, List<String> taskIds) {
            this.assignments = assignments;
            this.taskIds = taskIds;
        }

        /**
         * 获取派工记录列表
         *
         * @return 派工记录列表（包含机台ID、计划开始时间、计划结束时间、序号等）
         */
        public List<TaskAssignment> getAssignments() {
            return assignments;
        }

        /**
         * 获取任务ID列表
         *
         * @return 任务ID列表（用于批量更新任务状态：READY → SCHEDULED）
         */
        public List<String> getTaskIds() {
            return taskIds;
        }
    }

    /**
     * 机台运行时上下文（内存快照）
     *
     * 职责：在排程计算过程中维护机台的运行状态，避免频繁查询数据库
     *
     * 数据结构：
     * - nextAvailableTimeMap: 机台ID → 最早可用时间（前序任务结束时间）
     * - nextSequenceMap: 机台ID → 下一个序号
     *
     * 状态流转示例：
     * <pre>{@code
     * 初始状态（从数据库预加载）:
     * ┌──────────┬─────────────────────┬─────────┐
     * │ machineId │ nextAvailableTime   │ sequence│
     * ├──────────┼─────────────────────┼─────────┤
     * │ M001     │ 2026-03-31 10:00    │ 5       │
     * │ M002     │ null（无任务）      │ 1       │
     * └──────────┴─────────────────────┴─────────┘
     *
     * 处理任务1后（选择M001，08:00-12:00，序号5）:
     * ┌──────────┬─────────────────────┬─────────┐
     * │ machineId │ nextAvailableTime   │ sequence│
     * ├──────────┼─────────────────────┼─────────┤
     * │ M001     │ 2026-03-31 12:00 ← 更新│ 6 ← 更新│
     * │ M002     │ null                │ 1       │
     * └──────────┴─────────────────────┴─────────┘
     *
     * 处理任务2时，M002因为更早可用被选中:
     * ┌──────────┬─────────────────────┬─────────┐
     * │ machineId │ nextAvailableTime   │ sequence│
     * ├──────────┼─────────────────────┼─────────┤
     * │ M001     │ 12:00              │ 6       │
     * │ M002     │ 10:00 ← 更新        │ 2 ← 更新│
     * └──────────┴─────────────────────┴─────────┘
     * }</pre>
     *
     * 方法说明：
     * - getNextAvailableTime(): 获取机台最早可用时间（null表示从未派工）
     * - getNextSequence(): 获取下一个序号（不存在则返回1）
     * - update(): 更新机台状态（任务分配后调用）
     */
    public static class MachineRuntimeContext {
        /** 机台ID → 最早可用时间（前序任务结束时间） */
        private final Map<String, LocalDateTime> nextAvailableTimeMap = new HashMap<>();
        /** 机台ID → 下一个序号 */
        private final Map<String, Long> nextSequenceMap = new HashMap<>();

        /**
         * 获取机台的最早可用时间
         *
         * @param machineId 机台ID
         * @return 最早可用时间（null表示该机台从未被派工过）
         */
        public LocalDateTime getNextAvailableTime(String machineId) {
            return nextAvailableTimeMap.get(machineId);
        }

        /**
         * 获取机台的下一个序号
         *
         * @param machineId 机台ID
         * @return 下一个序号（如果该机台从未被派工，返回1）
         */
        public Long getNextSequence(String machineId) {
            return nextSequenceMap.getOrDefault(machineId, 1L);
        }

        /**
         * 更新机台运行状态
         *
         * 调用时机：任务分配到机台后立即更新
         *
         * 更新规则：
         * - 最早可用时间 = 本次任务的结束时间（下一个任务不能早于此时开始）
         * - 下一个序号 = 使用的序号 + 1（供下一个任务使用）
         *
         * @param machineId   机台ID
         * @param plannedEnd  任务计划结束时间（将成为机台的新最早可用时间）
         * @param usedSequence 本次使用的序号（下一个任务将使用 usedSequence + 1）
         */
        public void update(String machineId, LocalDateTime plannedEnd, Long usedSequence) {
            if (StringUtils.isNotEmpty(machineId) && plannedEnd != null) {
                nextAvailableTimeMap.put(machineId, plannedEnd);
            }
            if (StringUtils.isNotEmpty(machineId) && usedSequence != null) {
                nextSequenceMap.put(machineId, usedSequence + 1L);
            }
        }
    }
}
