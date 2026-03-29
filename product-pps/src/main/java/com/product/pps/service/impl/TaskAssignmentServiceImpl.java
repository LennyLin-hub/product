package com.product.pps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.constant.ResourceConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.utils.StringUtils;
import com.product.domain.dto.TaskAssignmentDTO;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
import com.product.domain.entity.TaskAssignment;
import com.product.domain.vo.TaskAssignmentVO;
import com.product.pps.mapper.TaskAssignmentMapper;
import com.product.pps.service.ITaskAssignmentService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;
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
    @Transactional(rollbackFor = Exception.class)
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
        return scheduleTasks(List.of(task), assignmentStart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean scheduleAll(TaskAssignmentDTO taskAssignmentDTO) {
        LocalDateTime assignmentStart = resolveAssignmentStart(taskAssignmentDTO);
        List<OperationTask> tasks = Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .orderByAsc(OperationTask::getBatchId, OperationTask::getSequence)
                .list();
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        return scheduleTasks(tasks, assignmentStart);
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
        for (OperationTask task : tasks) {
            if (task == null) {
                continue;
            }
            // 获取可用机台中最早可用时间
            MachineChoice choice = chooseMachine(task, machines, calendarMap, assignmentStart);
            if (choice == null) {
                return false;
            }
            TaskAssignment assignment = new TaskAssignment();
            assignment.setTaskId(task.getTaskId());
            assignment.setMachineId(choice.machineId);
            assignment.setPlannedStart(choice.plannedStart);
            assignment.setPlannedEnd(choice.plannedEnd);
            assignment.setSequenceOnResource(nextSequenceOnResource(choice.machineId));
            if (!save(assignment)) {
                return false;
            }
            boolean updated = Db.lambdaUpdate(OperationTask.class)
                    .set(OperationTask::getStatus, StatusConstants.SCHEDULED_OPERATION_TASK)
                    .eq(OperationTask::getTaskId, task.getTaskId())
                    .update();
            if (!updated) {
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
                                        LocalDateTime assignmentStart) {
        MachineChoice best = null;
        for (Resource machine : machines) {
            if (machine == null) {
                continue;
            }
            // 获取机器日历
            Calendar calendar = calendarMap.get(machine.getCalendarId());
            // 获取机器最早可用时间
            LocalDateTime machineNextTime = getMachineNextTime(machine.getResourceId());
            LocalDateTime candidate = maxTime(task.getEarliestStart(), machineNextTime, assignmentStart);
            LocalDateTime plannedStart = adjustToShiftStart(calendar, candidate);
            if (plannedStart == null) {
                continue;
            }
            long duration = task.getStdDurationMin() == null ? 0L : task.getStdDurationMin();
            TimeWindow window = adjustForShiftEnd(calendar, plannedStart, duration);
            if (window == null) {
                continue;
            }
            MachineChoice choice = new MachineChoice(machine.getResourceId(), window.start, window.end);
            if (best == null || choice.plannedStart.isBefore(best.plannedStart)) {
                best = choice;
            }
        }
        return best;
    }

    private LocalDateTime getMachineNextTime(String machineId) {
        return baseMapper.selectMachineNextTime(
                machineId,
                List.of(
                        StatusConstants.SCHEDULED_OPERATION_TASK,
                        StatusConstants.RUNNING_OPERATION_TASK,
                        StatusConstants.PAUSED_OPERATION_TASK
                )
        );
    }

    private Long nextSequenceOnResource(String machineId) {
        QueryWrapper<TaskAssignment> wrapper = new QueryWrapper<>();
        wrapper.select("MAX(sequence_on_resource) AS sequence_on_resource")
                .eq("machine_id", machineId);
        List<Object> result = baseMapper.selectObjs(wrapper);
        if (CollectionUtils.isEmpty(result) || result.get(0) == null) {
            return 1L;
        }
        Object value = result.get(0);
        if (value instanceof Number) {
            return ((Number) value).longValue() + 1L;
        }
        return 1L;
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
        // 将最早开始时间与班次日期进行比较
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

    private static class MachineChoice {
        private final String machineId;
        private final LocalDateTime plannedStart;
        private final LocalDateTime plannedEnd;

        private MachineChoice(String machineId, LocalDateTime plannedStart, LocalDateTime plannedEnd) {
            this.machineId = machineId;
            this.plannedStart = plannedStart;
            this.plannedEnd = plannedEnd;
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

}
