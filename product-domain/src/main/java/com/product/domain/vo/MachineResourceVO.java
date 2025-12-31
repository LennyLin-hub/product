package com.product.domain.vo;

import com.product.domain.entity.Resource;
import lombok.Data;

/**
 * @Auther: chuan
 * @Date: 2026/1/1 - 01 - 01 - 03:13
 * @Description: com.product.domain.vo
 * @version: 1.0
 */
@Data
public class MachineResourceVO extends Resource {
    private Integer tonnage;
    private String calendarName;
}
