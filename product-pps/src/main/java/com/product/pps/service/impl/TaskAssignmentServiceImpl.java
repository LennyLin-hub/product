package com.product.pps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.constant.ResourceConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.exception.ServiceException;
import com.product.common.utils.StringUtils;
import com.product.domain.dto.TaskAssignmentDTO;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
import com.product.domain.entity.TaskAssignment;
import com.product.domain.vo.TaskAssignmentVO;
import com.product.pps.dto.MachineRuntimeStatsDTO;
import com.product.pps.mapper.OperationTaskMapper;
import com.product.pps.mapper.TaskAssignmentMapper;
import com.product.pps.service.ITaskAssignmentService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 派工/排程结果Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2026-01-02
 */
@Service
public class TaskAssignmentServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment> implements ITaskAssignmentService {
    private static final int SCHEDULE_BATCH_SIZE = 200;

    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;
    @Autowired
    private OperationTaskMapper operationTaskMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    /**
     * 查询派工/排程结果
     *
     * @param assignmentId 派工/排程结果主键
     * @return 派工/排程结果
     */
    @Override
    public TaskAssignment selectTaskAssignmentByAssignmentId(Long assignmentId) {
        return getById(assignmentId);
    }

    /**
     * 查询派工/排程结果列表
     *
     * @param taskAssignment 查询条件
     * @return 派工/排程结果集合
     */
    @Override
    public List<TaskAssignment> selectTaskAssignmentList(TaskAssignment taskAssignment) {
        return list(buildQueryWrapper(taskAssignment));
    }

    /**
     * 分页查询派工/排程结果列表
     *
     * @param page      分页参数
     * @param taskAssignment 查询条件
     * @return 分页结果
     */
    @Override
    public Page<TaskAssignmentVO> selectTaskAssignmentPage(Page<TaskAssignmentVO> page, TaskAssignment taskAssignment) {
        return taskAssignmentMapper.selectTaskAssignmentPage(page, taskAssignment);
    }

    /**
     * 新增派工/排程结果
     *
     * @param taskAssignment 派工/排程结果
     * @return 是否成功
     */
    @Override
    public boolean insertTaskAssignment(TaskAssignment taskAssignment) {
        boolean saved = save(taskAssignment);
        return saved;
    }

    /**
     * 批量新增派工/排程结果
     *
     * @param taskAssignments 派工/排程结果列表
     * @return 成功条数
     */
    @Override
    public int batchInsertTaskAssignment(List<TaskAssignment> taskAssignments) {
        if (CollectionUtils.isEmpty(taskAssignments)) {
            return 0;
        }
        boolean success = saveBatch(taskAssignments);
        return success ? taskAssignments.size() : 0;
    }

    /**
     * 修改派工/排程结果
     *
     * @param taskAssignment 派工/排程结果
     * @return 是否成功
     */
    @Override
    public boolean updateTaskAssignment(TaskAssignment taskAssignment) {
        boolean updated = updateById(taskAssignment);
        return updated;
    }

    /**
     * 批量删除派工/排程结果
     *
     * @param assignmentIds 主键集合
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTaskAssignmentByAssignmentIds(Long[] assignmentIds) {
        if (assignmentIds == null || assignmentIds.length == 0) {
            return false;
        }
        List<String> taskIds = lambdaQuery().select(TaskAssignment::getTaskId)
                .in(TaskAssignment::getAssignmentId, assignmentIds)
                .list()
                .stream()
                .map(TaskAssignment::getTaskId)
                .distinct()
                .collect(Collectors.toList());
        // 撤销排程
        Db.lambdaUpdate(OperationTask.class).set(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .in(OperationTask::getTaskId, taskIds)
                .update();
        return removeByIds(Arrays.asList(assignmentIds));
    }

    /**
     * 删除派工/排程结果信息
     *
     * @param assignmentId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteTaskAssignmentByAssignmentId(Long assignmentId) {
        return removeById(assignmentId);
    }

    @Override
    public boolean schedule(TaskAssignmentDTO taskAssignmentDTO) {
        if (taskAssignmentDTO == null || StringUtils.isEmpty(taskAssignmentDTO.getTaskId())) {
            return scheduleAll(taskAssignmentDTO);
        }
        OperationTask task = Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getTaskId, taskAssignmentDTO.getTaskId())
                .one();
        if (task == null) {
            return false;
        }
        if (!StatusConstants.READY_OPERATION_TASK.equals(task.getStatus())) {
            return false;
        }
        LocalDateTime assignmentStart = resolveAssignmentStart(taskAssignmentDTO);
        // 单任务排程保持原子性，避免 assignment 已落库但任务状态未更新。
        Boolean scheduled = transactionTemplate.execute(status -> scheduleTasks(List.of(task), assignmentStart));
        return Boolean.TRUE.equals(scheduled);
    }

    @Override
    public boolean scheduleAll(TaskAssignmentDTO taskAssignmentDTO) {
        // 获取前端传入的开始时间
        LocalDateTime assignmentStart = resolveAssignmentStart(taskAssignmentDTO);
        // 获取状态良好的机器
        List<Resource> machines = loadAvailableMachines(assignmentStart);
        if (CollectionUtils.isEmpty(machines)) {
            return false;
        }
        // 加载机器对应的日历
        Map<Long, Calendar> calendarMap = loadCalendarMap(machines);
        // 先做一次资源占用快照，后续批次在内存中滚动推进，避免循环查库。
        MachineRuntimeContext runtimeContext = buildMachineRuntimeContext(machines);
        List<ScheduleBatchResult> batchResults = new ArrayList<>();
        long current = 1L;
        while (true) {
            // 计算阶段按页推进，避免一次把所有 READY 任务全部加载进内存。
            Page<OperationTask> page = new Page<>(current, SCHEDULE_BATCH_SIZE, false);
            Page<OperationTask> taskPage = Db.lambdaQuery(OperationTask.class)
                    .eq(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                    .orderByAsc(OperationTask::getBatchId, OperationTask::getSequence, OperationTask::getTaskId)
                    .page(page);
            List<OperationTask> tasks = taskPage.getRecords();
            if (CollectionUtils.isEmpty(tasks)) {
                break;
            }
            ScheduleBatchResult batchResult = calculateBatchAssignments(tasks, machines, calendarMap, runtimeContext, assignmentStart);
            batchResults.add(batchResult);
            if (!taskPage.hasNext()) {
                break;
            }
            current++;
        }
        if (CollectionUtils.isEmpty(batchResults)) {
            return true;
        }
        // 落库阶段统一放到一个事务里，保证 scheduleAll 仍然是全有或全无。
        Boolean persisted = transactionTemplate.execute(status -> persistAllBatchResults(batchResults));
        return Boolean.TRUE.equals(persisted);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<TaskAssignment> buildQueryWrapper(TaskAssignment taskAssignment) {
        LambdaQueryWrapper<TaskAssignment> wrapper = new LambdaQueryWrapper<>();
        if (taskAssignment == null) {
            return wrapper;
        }
        wrapper.eq(taskAssignment.getTaskId() != null, TaskAssignment::getTaskId, taskAssignment.getTaskId());
        wrapper.eq(taskAssignment.getMachineId() != null, TaskAssignment::getMachineId, taskAssignment.getMachineId());
        wrapper.eq(taskAssignment.getPlannedStart() != null, TaskAssignment::getPlannedStart, taskAssignment.getPlannedStart());
        wrapper.eq(taskAssignment.getPlannedEnd() != null, TaskAssignment::getPlannedEnd, taskAssignment.getPlannedEnd());
        wrapper.eq(taskAssignment.getSequenceOnResource() != null, TaskAssignment::getSequenceOnResource, taskAssignment.getSequenceOnResource());
        return wrapper;
    }

    private boolean scheduleTasks(List<OperationTask> tasks, LocalDateTime assignmentStart) {
        // 加载所有可用的机台
        List<Resource> machines = loadAvailableMachines(assignmentStart);
        if (CollectionUtils.isEmpty(machines)) {
            return false;
        }
        // 先读取机台对应日历id，然后获取日历对象
        Map<Long, Calendar> calendarMap = loadCalendarMap(machines);
        MachineRuntimeContext runtimeContext = buildMachineRuntimeContext(machines);
        ScheduleBatchResult batchResult = calculateBatchAssignments(tasks, machines, calendarMap, runtimeContext, assignmentStart);
        return persistBatchResult(batchResult);
    }

    private ScheduleBatchResult calculateBatchAssignments(List<OperationTask> tasks,
                                                          List<Resource> machines,
                                                          Map<Long, Calendar> calendarMap,
                                                          MachineRuntimeContext runtimeContext,
                                                          LocalDateTime assignmentStart) {
        List<TaskAssignment> assignments = new ArrayList<>(tasks.size());
        List<String> taskIds = new ArrayList<>(tasks.size());
        for (OperationTask task : tasks) {
            if (task == null) {
                continue;
            }
            // 获取可用机台中最早可用时间
            MachineChoice choice = chooseMachine(task, machines, calendarMap, assignmentStart, runtimeContext);
            if (choice == null) {
                throw new ServiceException("任务" + task.getTaskId() + "没有可用机台");
            }
            TaskAssignment assignment = new TaskAssignment();
            assignment.setTaskId(task.getTaskId());
            assignment.setMachineId(choice.machineId);
            assignment.setPlannedStart(choice.plannedStart);
            assignment.setPlannedEnd(choice.plannedEnd);
            assignment.setSequenceOnResource(choice.sequenceOnResource);
            assignments.add(assignment);
            taskIds.add(task.getTaskId());
            // 当前批次内不回写数据库，直接推进内存态的机台可用时间与序号。
            runtimeContext.update(choice.machineId, choice.plannedEnd, choice.sequenceOnResource);
        }
        return new ScheduleBatchResult(assignments, taskIds);
    }

    private boolean persistBatchResult(ScheduleBatchResult batchResult) {
        if (batchResult == null || CollectionUtils.isEmpty(batchResult.assignments)) {
            return true;
        }
        boolean saved = saveBatch(batchResult.assignments, SCHEDULE_BATCH_SIZE);
        if (!saved) {
            return false;
        }
        // 仅允许 READY -> SCHEDULED，避免并发下把已变更状态的任务静默覆盖。
        int updated = operationTaskMapper.batchMarkScheduled(
                batchResult.taskIds,
                StatusConstants.READY_OPERATION_TASK,
                StatusConstants.SCHEDULED_OPERATION_TASK
        );
        if (updated != batchResult.taskIds.size()) {
            throw new ServiceException("任务状态更新失败，预期更新" + batchResult.taskIds.size() + "条，实际更新" + updated + "条");
        }
        return true;
    }

    private boolean persistAllBatchResults(List<ScheduleBatchResult> batchResults) {
        for (ScheduleBatchResult batchResult : batchResults) {
            if (!persistBatchResult(batchResult)) {
                return false;
            }
        }
        return true;
    }

    private LocalDateTime resolveAssignmentStart(TaskAssignmentDTO taskAssignmentDTO) {
        if (taskAssignmentDTO != null && taskAssignmentDTO.getAssignmentStart() != null) {
            return taskAssignmentDTO.getAssignmentStart();
        }
        return LocalDateTime.now();
    }

    private List<Resource> loadAvailableMachines(LocalDateTime assignmentStart) {
        List<Resource> resources = Db.lambdaQuery(Resource.class)
                .eq(Resource::getResourceType, ResourceConstants.RESOURCE_TYPE_MACHINE)
                .eq(Resource::getStatus, StatusConstants.AVAILABLE_RESOURCE_STATUS)
                .list();
        if (CollectionUtils.isEmpty(resources)) {
            return resources;
        }
        return resources.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Long, Calendar> loadCalendarMap(List<Resource> machines) {
        Set<Long> calendarIds = new HashSet<>();
        for (Resource machine : machines) {
            if (machine != null && machine.getCalendarId() != null) {
                calendarIds.add(machine.getCalendarId());
            }
        }
        if (calendarIds.isEmpty()) {
            return new HashMap<>();
        }
        return Db.lambdaQuery(Calendar.class)
                .in(Calendar::getCalendarId, calendarIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Calendar::getCalendarId, item -> item, (a, b) -> a));
    }

    private MachineChoice chooseMachine(OperationTask task,
                                        List<Resource> machines,
                                        Map<Long, Calendar> calendarMap,
                                        LocalDateTime assignmentStart,
                                        MachineRuntimeContext runtimeContext) {
        MachineChoice best = null;
        for (Resource machine : machines) {
            if (machine == null) {
                continue;
            }
            // 获取机器日历
            Calendar calendar = calendarMap.get(machine.getCalendarId());
            // 获取机器最早可用时间
            LocalDateTime machineNextTime = runtimeContext.getNextAvailableTime(machine.getResourceId());
            LocalDateTime candidate = maxTime(task.getEarliestStart(), machineNextTime, assignmentStart);
            LocalDateTime plannedStart = adjustToShiftStart(calendar, candidate);
            if (plannedStart == null) {
                continue;
            }
            // 取任务时长
            long duration = task.getStdDurationMin() == null ? 0L : task.getStdDurationMin();
            /**
             * 算实际时间窗口
             * 判断是否会跨下班时间，如果会需要根据班次规则把任务整体顺延到下一工作日班次开始
             */
            TimeWindow window = adjustForShiftEnd(calendar, plannedStart, duration);
            if (window == null) {
                continue;
            }
            Long sequenceOnResource = runtimeContext.getNextSequence(machine.getResourceId());
            MachineChoice choice = new MachineChoice(machine.getResourceId(), window.start, window.end, sequenceOnResource);
            // 如果没有最佳方案，直接把该机台当作最佳方案
            // 如果有最佳方案则比较谁的开始时间更早
            if (best == null
                    || choice.plannedStart.isBefore(best.plannedStart)
                    || (choice.plannedStart.isEqual(best.plannedStart) && choice.plannedEnd.isBefore(best.plannedEnd))) {
                best = choice;
            }
        }
        // 返回最早开始的机台
        return best;
    }

    private MachineRuntimeContext buildMachineRuntimeContext(List<Resource> machines) {
        // 获取机器id集合
        List<String> machineIds = machines.stream()
                .map(Resource::getResourceId)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        MachineRuntimeContext context = new MachineRuntimeContext();
        if (CollectionUtils.isEmpty(machineIds)) {
            return context;
        }
        // 预加载每台机的最近占用结束时间，后续排程直接走内存快照。
        List<MachineRuntimeStatsDTO> latestEndTimes = baseMapper.selectMachineLatestEndTime(
                machineIds,
                List.of(
                        StatusConstants.SCHEDULED_OPERATION_TASK,
                        StatusConstants.RUNNING_OPERATION_TASK,
                        StatusConstants.PAUSED_OPERATION_TASK
                )
        );
        for (MachineRuntimeStatsDTO row : latestEndTimes) {
            String machineId = row.getMachineId();
            LocalDateTime latestEndTime = row.getLatestEndTime();
            if (StringUtils.isNotEmpty(machineId) && latestEndTime != null) {
                context.nextAvailableTimeMap.put(machineId, latestEndTime);
            }
        }
        // 预加载资源上的最大顺序号，避免每次落点都重新聚合 task_assignment。
        List<MachineRuntimeStatsDTO> maxSequences = baseMapper.selectMachineMaxSequence(machineIds);
        for (MachineRuntimeStatsDTO row : maxSequences) {
            String machineId = row.getMachineId();
            Long maxSequence = row.getMaxSequence();
            if (StringUtils.isNotEmpty(machineId) && maxSequence != null) {
                context.nextSequenceMap.put(machineId, maxSequence + 1L);
            }
        }
        return context;
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
        // 将最早开始时间与班次日期进行比较获取最早的工作日
        date = nextWorkday(calendar, date);
        // 获取该日的班次开始时间
        LocalDateTime shiftStartTime = LocalDateTime.of(date, startTime);
        // 获取该日的班次结束时间
        LocalDateTime shiftEndTime = LocalDateTime.of(date, endTime);
        // 如果传入时间在班次开始时间前，那么直接返回开始时间
        if (time.isBefore(shiftStartTime)) {
            return shiftStartTime;
        }
        // 如果时间在班次开始时间后，则需要判断是否在班次结束时间前
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

    private boolean isOffShift(Calendar calendar, LocalDateTime now) {
        if (calendar == null || now == null) {
            return true;
        }
        String shiftStart = calendar.getShiftStart();
        String shiftEnd = calendar.getShiftEnd();
        if (StringUtils.isEmpty(shiftStart) || StringUtils.isEmpty(shiftEnd)) {
            return true;
        }
        if (!isWorkday(calendar.getWorkdayPattern(), now.getDayOfWeek())) {
            return true;
        }
        LocalTime nowTime;
        LocalTime startTime;
        LocalTime endTime;
        try {
            nowTime = now.toLocalTime();
            startTime = LocalTime.parse(shiftStart);
            endTime = LocalTime.parse(shiftEnd);
        } catch (Exception ex) {
            return true;
        }
        return nowTime.isBefore(startTime) || !nowTime.isBefore(endTime);
    }

    private LocalDate nextWorkday(Calendar calendar, LocalDate date) {
        if (calendar == null || date == null) {
            return date;
        }
        String workdayPattern = calendar.getWorkdayPattern();
        LocalDate next = date;
        int guard = 0;
        // 将传入日期与班次进行比较，获取最早的工作日
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
            // 不跨周
            if (startValue <= endValue) {
                return dayValue >= startValue && dayValue <= endValue;
            }
            // 跨周
            return dayValue >= startValue || dayValue <= endValue;
        }
        // 工作日为单值时直接判断是否匹配
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

    private static class MachineChoice {
        private final String machineId;
        private final LocalDateTime plannedStart;
        private final LocalDateTime plannedEnd;
        private final Long sequenceOnResource;

        private MachineChoice(String machineId, LocalDateTime plannedStart, LocalDateTime plannedEnd, Long sequenceOnResource) {
            this.machineId = machineId;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
            this.sequenceOnResource = sequenceOnResource;
        }
    }

    private static class TimeWindow {
        private final LocalDateTime start;
        private final LocalDateTime end;

        private TimeWindow(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class ScheduleBatchResult {
        private final List<TaskAssignment> assignments;
        private final List<String> taskIds;

        private ScheduleBatchResult(List<TaskAssignment> assignments, List<String> taskIds) {
            this.assignments = assignments;
            this.taskIds = taskIds;
        }
    }

    private static class MachineRuntimeContext {
        // 机器最近可用时间集合
        private final Map<String, LocalDateTime> nextAvailableTimeMap = new HashMap<>();
        private final Map<String, Long> nextSequenceMap = new HashMap<>();

        private LocalDateTime getNextAvailableTime(String machineId) {
            return nextAvailableTimeMap.get(machineId);
        }

        private Long getNextSequence(String machineId) {
            return nextSequenceMap.getOrDefault(machineId, 1L);
        }

        private void update(String machineId, LocalDateTime plannedEnd, Long usedSequence) {
            if (StringUtils.isNotEmpty(machineId) && plannedEnd != null) {
                nextAvailableTimeMap.put(machineId, plannedEnd);
            }
            if (StringUtils.isNotEmpty(machineId) && usedSequence != null) {
                nextSequenceMap.put(machineId, usedSequence + 1L);
            }
        }
    }

}
