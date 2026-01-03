package com.product.domain.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @Auther: chuan
 * @Date: 2026/1/3 - 01 - 03 - 00:07
 * @Description: com.product.domain.dto
 * @version: 1.0
 */
@Data
public class TaskAssignmentDTO {
    private String taskId;
    @DateTimeFormat(pattern = "yyyy-MM-dd[' 'HH:mm:ss']")
    private LocalDateTime assignmentStart;
}
