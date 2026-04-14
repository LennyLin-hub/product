package com.product.pps.service.impl;

import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
import com.product.pps.dto.TaskSchedulingPriorityDTO;
import com.product.pps.enums.SchedulingStrategy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskSchedulingCalculatorTest {

    private final TaskSchedulingCalculator calculator = new TaskSchedulingCalculator();

    /** EARLIEST_FINISH 策略：工期短的任务应排在前面（标准排序 vs 实际排序验证） */
    @Test
    void orderTasksShouldPrioritizeEarliestFinishWhenRequested() {
        OperationTask slowButEarly = new OperationTask();
        slowButEarly.setTaskId("T1");
        slowButEarly.setEarliestStart(LocalDateTime.of(2026, 4, 1, 8, 0));
        slowButEarly.setStdDurationMin(240L);
        slowButEarly.setSequence(1L);

        OperationTask fastButLate = new OperationTask();
        fastButLate.setTaskId("T2");
        fastButLate.setEarliestStart(LocalDateTime.of(2026, 4, 1, 9, 0));
        fastButLate.setStdDurationMin(60L);
        fastButLate.setSequence(2L);

        List<OperationTask> ordered = calculator.orderTasks(List.of(slowButEarly, fastButLate), SchedulingStrategy.EARLIEST_FINISH, Map.of());

        assertEquals("T2", ordered.get(0).getTaskId());
        assertEquals("T1", ordered.get(1).getTaskId());
    }

    /** DUE_DATE_PRIORITY 策略：交期早的任务应排在前面，交期相同时优先级数值大的优先 */
    @Test
    void orderTasksShouldUseDueDatePriorityContext() {
        OperationTask task1 = new OperationTask();
        task1.setTaskId("T1");
        task1.setEarliestStart(LocalDateTime.of(2026, 4, 1, 8, 0));
        task1.setStdDurationMin(60L);
        task1.setSequence(1L);

        OperationTask task2 = new OperationTask();
        task2.setTaskId("T2");
        task2.setEarliestStart(LocalDateTime.of(2026, 4, 1, 8, 0));
        task2.setStdDurationMin(60L);
        task2.setSequence(2L);

        TaskSchedulingPriorityDTO priority1 = new TaskSchedulingPriorityDTO();
        priority1.setTaskId("T1");
        priority1.setDueDate(LocalDateTime.of(2026, 4, 3, 0, 0));
        priority1.setPriority(1L);

        TaskSchedulingPriorityDTO priority2 = new TaskSchedulingPriorityDTO();
        priority2.setTaskId("T2");
        priority2.setDueDate(LocalDateTime.of(2026, 4, 2, 0, 0));
        priority2.setPriority(5L);

        List<OperationTask> ordered = calculator.orderTasks(
                List.of(task1, task2),
                SchedulingStrategy.DUE_DATE_PRIORITY,
                Map.of("T1", priority1, "T2", priority2));

        assertEquals("T2", ordered.get(0).getTaskId());
        assertEquals("T1", ordered.get(1).getTaskId());
    }

    /** 验证不同策略会选到不同机台：EARLIEST_START 选最早开始的 M1，EARLIEST_FINISH 选最早完工的 M2 */
    @Test
    void calculateBatchAssignmentsShouldChooseDifferentMachinesForDifferentStrategies() {
        OperationTask task = new OperationTask();
        task.setTaskId("T1");
        task.setEarliestStart(LocalDateTime.of(2026, 4, 10, 7, 0));
        task.setStdDurationMin(120L);

        Resource earlyMachine = new Resource();
        earlyMachine.setResourceId("M1");
        earlyMachine.setCalendarId(1L);

        Resource lateMachine = new Resource();
        lateMachine.setResourceId("M2");
        lateMachine.setCalendarId(2L);

        Calendar shortShift = new Calendar();
        shortShift.setCalendarId(1L);
        shortShift.setWorkdayPattern("Mon-Tue-Wed-Thu-Fri-Sat-Sun");
        shortShift.setShiftStart("08:00");
        shortShift.setShiftEnd("09:00");

        Calendar longShift = new Calendar();
        longShift.setCalendarId(2L);
        longShift.setWorkdayPattern("Mon-Tue-Wed-Thu-Fri-Sat-Sun");
        longShift.setShiftStart("08:00");
        longShift.setShiftEnd("17:00");

        TaskSchedulingCalculator.MachineRuntimeContext context = new TaskSchedulingCalculator.MachineRuntimeContext();
        context.update("M2", LocalDateTime.of(2026, 4, 10, 9, 0), 1L);

        TaskSchedulingCalculator.ScheduleBatchResult earliestStart = calculator.calculateBatchAssignments(
                List.of(task),
                List.of(earlyMachine, lateMachine),
                Map.of(1L, shortShift, 2L, longShift),
                context,
                LocalDateTime.of(2026, 4, 10, 7, 0),
                SchedulingStrategy.EARLIEST_START);

        assertNotNull(earliestStart);
        assertEquals("M1", earliestStart.getAssignments().get(0).getMachineId());

        TaskSchedulingCalculator.MachineRuntimeContext finishContext = new TaskSchedulingCalculator.MachineRuntimeContext();
        finishContext.update("M2", LocalDateTime.of(2026, 4, 10, 9, 0), 1L);

        TaskSchedulingCalculator.ScheduleBatchResult earliestFinish = calculator.calculateBatchAssignments(
                List.of(task),
                List.of(earlyMachine, lateMachine),
                Map.of(1L, shortShift, 2L, longShift),
                finishContext,
                LocalDateTime.of(2026, 4, 10, 7, 0),
                SchedulingStrategy.EARLIEST_FINISH);

        assertNotNull(earliestFinish);
        assertEquals("M2", earliestFinish.getAssignments().get(0).getMachineId());
    }
}
