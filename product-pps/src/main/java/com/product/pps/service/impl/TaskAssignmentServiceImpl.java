package com.product.pps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.common.annotation.BizIdPrefix;
import com.product.common.utils.StringUtils;
import com.product.common.utils.uuid.IdUtils;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
import java.util.List;

import com.product.domain.entity.TaskAssignment;
import com.product.pps.mapper.TaskAssignmentMapper;
import com.product.pps.service.ITaskAssignmentService;

/**
 * 派工/排程结果Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2026-01-02
 */
@Service
public class TaskAssignmentServiceImpl extends ServiceImpl<TaskAssignmentMapper, TaskAssignment> implements ITaskAssignmentService {

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
    public Page<TaskAssignment> selectTaskAssignmentPage(Page<TaskAssignment> page, TaskAssignment taskAssignment) {
        return this.page(page, buildQueryWrapper(taskAssignment));
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
    public boolean deleteTaskAssignmentByAssignmentIds(Long[] assignmentIds) {
        if (assignmentIds == null || assignmentIds.length == 0) {
            return false;
        }
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


}
