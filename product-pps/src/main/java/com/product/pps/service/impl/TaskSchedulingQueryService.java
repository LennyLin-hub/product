package com.product.pps.service.impl;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.constant.ResourceConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.utils.StringUtils;
import com.product.domain.entity.CustomerOrder;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.OrderLine;
import com.product.domain.entity.ProductionBatch;
import com.product.domain.entity.Resource;
import com.product.pps.dto.TaskSchedulingPriorityDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排程查询服务。
 *
 * 说明：负责待排任务、可用机台、班次日历的读取与分页加载。
 */
@Slf4j
@Component
public class TaskSchedulingQueryService {

    public List<Resource> loadAvailableMachines() {
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

    public Map<Long, Calendar> loadCalendarMap(List<Resource> machines) {
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

    public int countReadyTasks() {
        Long count = Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .count();
        return count == null ? 0 : count.intValue();
    }

    /** 加载所有 READY 状态的任务列表（由 Coordinator 按策略排序后分批计算） */
    public List<OperationTask> loadReadyTaskList() {
        return Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .list();
    }

    public OperationTask loadTaskByTaskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }
        return Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getTaskId, taskId)
                .one();
    }

    /**
     * 加载任务的优先级上下文（交期 + 优先级）。
     *
     * 数据链路：OperationTask → ProductionBatch → OrderLine → CustomerOrder
     * 从订单中提取 dueDate（交期）和 priority（优先级），用于 DUE_DATE_PRIORITY 策略排序。
     *
     * @param tasks 待排程任务列表
     * @return 任务ID → 优先级上下文（找不到关联订单时交期和优先级为 null）
     */
    public Map<String, TaskSchedulingPriorityDTO> loadTaskPriorityMap(List<OperationTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return new HashMap<>();
        }

        Set<String> batchIds = tasks.stream()
                .map(OperationTask::getBatchId)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (batchIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ProductionBatch> batches = Db.lambdaQuery(ProductionBatch.class)
                .select(ProductionBatch::getBatchId, ProductionBatch::getOrderLineId)
                .in(ProductionBatch::getBatchId, batchIds)
                .list();
        if (CollectionUtils.isEmpty(batches)) {
            return new HashMap<>();
        }

        Map<String, Long> batchToOrderLine = batches.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotEmpty(item.getBatchId()) && item.getOrderLineId() != null)
                .collect(Collectors.toMap(ProductionBatch::getBatchId, ProductionBatch::getOrderLineId, (a, b) -> a));

        Set<Long> orderLineIds = new HashSet<>(batchToOrderLine.values());
        List<OrderLine> orderLines = Db.lambdaQuery(OrderLine.class)
                .select(OrderLine::getOrderLineId, OrderLine::getOrderId)
                .in(OrderLine::getOrderLineId, orderLineIds)
                .list();
        Map<Long, String> orderLineToOrderId = orderLines.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getOrderLineId() != null && StringUtils.isNotEmpty(item.getOrderId()))
                .collect(Collectors.toMap(OrderLine::getOrderLineId, OrderLine::getOrderId, (a, b) -> a));

        Set<String> orderIds = new HashSet<>(orderLineToOrderId.values());
        List<CustomerOrder> orders = Db.lambdaQuery(CustomerOrder.class)
                .select(CustomerOrder::getOrderId, CustomerOrder::getDueDate, CustomerOrder::getPriority)
                .in(CustomerOrder::getOrderId, orderIds)
                .list();
        Map<String, CustomerOrder> orderMap = orders.stream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotEmpty(item.getOrderId()))
                .collect(Collectors.toMap(CustomerOrder::getOrderId, item -> item, (a, b) -> a));

        Map<String, TaskSchedulingPriorityDTO> priorityMap = new HashMap<>();
        for (OperationTask task : tasks) {
            if (task == null || StringUtils.isEmpty(task.getTaskId())) {
                continue;
            }
            TaskSchedulingPriorityDTO context = new TaskSchedulingPriorityDTO();
            context.setTaskId(task.getTaskId());
            context.setBatchId(task.getBatchId());

            Long orderLineId = task.getBatchId() == null ? null : batchToOrderLine.get(task.getBatchId());
            if (orderLineId != null) {
                String orderId = orderLineToOrderId.get(orderLineId);
                if (StringUtils.isNotEmpty(orderId)) {
                    CustomerOrder order = orderMap.get(orderId);
                    if (order != null) {
                        context.setDueDate(order.getDueDate());
                        context.setPriority(order.getPriority());
                    }
                }
            }
            priorityMap.put(task.getTaskId(), context);
        }
        return priorityMap;
    }
}
