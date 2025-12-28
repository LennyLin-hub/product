package com.product.demand.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.product.domain.entity.Product;

import java.util.List;

/**
 * 产品Service接口（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-23
 */
public interface IProductService extends IService<Product> {

    /**
     * 查询产品
     *
     * @param productId 产品主键
     * @return 产品
     */
    Product selectProductByProductId(Long productId);

    /**
     * 查询产品列表
     *
     * @param product 查询条件
     * @return 产品集合
     */
    List<Product> selectProductList(Product product);

    /**
     * 分页查询产品列表
     *
     * @param page      分页参数
     * @param product 查询条件
     * @return 分页结果
     */
    Page<Product> selectProductPage(Page<Product> page, Product product);

    /**
     * 新增产品
     *
     * @param product 产品
     * @return 是否成功
     */
    boolean insertProduct(Product product);

    /**
     * 批量新增产品
     *
     * @param products 产品列表
     * @return 成功条数
     */
    int batchInsertProduct(List<Product> products);

    /**
     * 修改产品
     *
     * @param product 产品
     * @return 是否成功
     */
    boolean updateProduct(Product product);

    /**
     * 批量删除产品
     *
     * @param productIds 主键集合
     * @return 是否成功
     */
    boolean deleteProductByProductIds(Long[] productIds);

    /**
     * 删除产品信息
     *
     * @param productId 主键
     * @return 是否成功
     */
    boolean deleteProductByProductId(Long productId);

    IPage<Product> selectCustomerPage();
}
