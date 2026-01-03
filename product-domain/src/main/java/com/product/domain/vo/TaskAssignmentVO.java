package com.product.domain.vo;

import com.product.domain.entity.TaskAssignment;
import lombok.Data;

/**
 * @Auther: chuan
 * @Date: 2026/1/3 - 01 - 03 - 01:08
 * @Description: com.product.domain.vo
 * @version: 1.0
 */
@Data
public class TaskAssignmentVO extends TaskAssignment {
    private String batchId;
    private String opCode;
}
