package com.product.master.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.product.common.annotation.BizIdPrefix;
import com.product.common.constant.ResourceConstants;
import com.product.common.constant.StatusConstants;
import com.product.common.utils.StringUtils;
import com.product.common.utils.uuid.IdUtils;
import com.product.domain.dto.MachineResource;
import com.product.domain.entity.Resource;
import com.product.domain.vo.MachineResourceVO;
import com.product.master.domain.entity.Machine;
import com.product.master.resource.mapper.MachineMapper;
import com.product.master.resource.service.IMachineService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 注塑机扩展信息Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2026-01-01
 */
@Service
public class MachineServiceImpl extends ServiceImpl<MachineMapper, Machine> implements IMachineService {

    @Autowired
    private MachineMapper machineMapper;

    /**
     * 查询注塑机扩展信息
     *
     * @param machineId 注塑机扩展信息主键
     * @return 注塑机扩展信息
     */
    @Override
    public Machine selectMachineByMachineId(String machineId) {
        return getById(machineId);
    }

    /**
     * 查询注塑机扩展信息列表
     *
     * @param machine 查询条件
     * @return 注塑机扩展信息集合
     */
    @Override
    public List<Machine> selectMachineList(Machine machine) {
        return list(buildQueryWrapper(machine));
    }

    /**
     * 分页查询注塑机扩展信息列表
     *
     * @param page            分页参数
     * @param machineResource 查询条件
     * @return 分页结果
     */
    @Override
    public Page<Machine> selectMachinePage(Page<MachineResourceVO> page, MachineResource machineResource) {
        return machineMapper.selectMachinePage(page, machineResource);
    }

    /**
     * 新增注塑机扩展信息
     *
     * @param machineResource 注塑机扩展信息
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertMachine(MachineResource machineResource) {
        Machine machine = new Machine();
        BeanUtils.copyProperties(machineResource, machine);
        // 需要插入资源表
        Resource resource = new Resource();
        BeanUtils.copyProperties(machineResource, resource);
        resource.setResourceId(buildBizId(resource));
        machine.setMachineId(resource.getResourceId());
        resource.setResourceType(ResourceConstants.RESOURCE_TYPE_MACHINE);
        resource.setStatus(StatusConstants.AVAILABLE_RESOURCE_STATUS);
        boolean saveResource = Db.save(resource);
        boolean saveMachine = save(machine);
        return saveResource && saveMachine;
    }

    /**
     * 批量新增注塑机扩展信息
     *
     * @param machines 注塑机扩展信息列表
     * @return 成功条数
     */
    @Override
    public int batchInsertMachine(List<Machine> machines) {
        if (CollectionUtils.isEmpty(machines)) {
            return 0;
        }
        machines.forEach(item -> {
            if (StringUtils.isEmpty(item.getMachineId())) {
                item.setMachineId(buildBizId(item));
            }
        });
        boolean success = saveBatch(machines);
        return success ? machines.size() : 0;
    }

    /**
     * 修改注塑机扩展信息
     *
     * @param machine 注塑机扩展信息
     * @return 是否成功
     */
    @Override
    public boolean updateMachine(Machine machine) {
        boolean updated = updateById(machine);
        return updated;
    }

    /**
     * 批量删除注塑机扩展信息
     *
     * @param machineIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteMachineByMachineIds(String[] machineIds) {
        if (machineIds == null || machineIds.length == 0) {
            return false;
        }
        return removeByIds(Arrays.asList(machineIds));
    }

    /**
     * 删除注塑机扩展信息信息
     *
     * @param machineId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteMachineByMachineId(String machineId) {
        return removeById(machineId);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Machine> buildQueryWrapper(Machine machine) {
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<>();
        if (machine == null) {
            return wrapper;
        }
        wrapper.eq(machine.getTonnage() != null, Machine::getTonnage, machine.getTonnage());
        wrapper.eq(machine.getDefaultSetupTimeMin() != null, Machine::getDefaultSetupTimeMin, machine.getDefaultSetupTimeMin());
        return wrapper;
    }


    private String buildBizId(Object entity) {
        BizIdPrefix annotation = entity.getClass().getAnnotation(BizIdPrefix.class);
        String prefix = annotation != null ? annotation.value() : null;
        String suffix = IdUtils.simpleUUID();
        return StringUtils.isNotEmpty(prefix) ? prefix + suffix : suffix;
    }
}
