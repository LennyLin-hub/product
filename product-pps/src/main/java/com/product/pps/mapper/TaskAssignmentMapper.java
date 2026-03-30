package com.product.pps.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.domain.entity.TaskAssignment;
import com.product.domain.vo.TaskAssignmentVO;
import com.product.pps.dto.MachineRuntimeStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派工/排程结果Mapper接口，基于 MyBatis-Plus
 *
 * @author product
 * @date 2026-01-02
 */
@Mapper
public interface TaskAssignmentMapper extends BaseMapper<TaskAssignment> {
    LocalDateTime selectMachineNextTime(@Param("machineId") String machineId,
                                        @Param("statusList") List<String> statusList);

    List<MachineRuntimeStatsDTO> selectMachineLatestEndTime(@Param("machineIds") List<String> machineIds,
                                                            @Param("statusList") List<String> statusList);

    List<MachineRuntimeStatsDTO> selectMachineMaxSequence(@Param("machineIds") List<String> machineIds);

    Page<TaskAssignmentVO> selectTaskAssignmentPage(Page<TaskAssignmentVO> page,
                                                    @Param("taskAssignment") TaskAssignment taskAssignment);
}
