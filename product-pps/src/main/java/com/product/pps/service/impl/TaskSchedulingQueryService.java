package com.product.pps.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.constant.ResourceConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.utils.StringUtils;
import com.product.domain.entity.Calendar;
import com.product.domain.entity.OperationTask;
import com.product.domain.entity.Resource;
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

    // TODO: 排序需要修改，当前按照批次ID来排序，批次UUID无序等同于随机排序
    public Page<OperationTask> loadReadyTaskPage(long pageNum, int pageSize) {
        Page<OperationTask> page = new Page<>(pageNum, pageSize, false);
        return Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getStatus, StatusConstants.READY_OPERATION_TASK)
                .orderByAsc(OperationTask::getBatchId, OperationTask::getSequence, OperationTask::getTaskId)
                .page(page);
    }

    public OperationTask loadTaskByTaskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }
        return Db.lambdaQuery(OperationTask.class)
                .eq(OperationTask::getTaskId, taskId)
                .one();
    }
}
