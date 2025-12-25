package com.product.domain.vo;

import com.product.domain.entity.CustomerOrder;
import lombok.Data;

/**
 * @Auther: chuan
 * @Date: 2025/12/25 - 12 - 25 - 22:41
 * @Description: com.product.domain.vo
 * @version: 1.0
 */
@Data
public class CustomerOrderVO extends CustomerOrder {
    private String customerName;
}
