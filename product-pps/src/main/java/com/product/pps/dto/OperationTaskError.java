package com.product.pps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成任务错误信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationTaskError {
    private String batchId;
    private String errorMessage;
}
