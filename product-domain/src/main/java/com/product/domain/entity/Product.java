package com.product.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.product.common.annotation.Excel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.product.common.core.entity.BaseEntity;

/**
 * 产品对象 product
 *
 * @author product
 * @date 2025-12-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 产品id */
    @TableId(value = "product_id", type = IdType.AUTO )
    private Long productId;

    /** 产品名称 */
    @Excel(name = "产品名称")
    @TableField(value = "product_name")
    private String productName;

    /** 图片 */
    @Excel(name = "图片")
    @TableField(value = "image")
    private String image;

}
