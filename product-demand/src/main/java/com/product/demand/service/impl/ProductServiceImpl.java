package com.product.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.product.common.utils.StringUtils;
import com.product.demand.mapper.ProductMapper;
import com.product.demand.service.IProductService;
import com.product.domain.entity.Product;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 产品Service业务层处理（MyBatis-Plus）
 *
 * @author product
 * @date 2025-12-23
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    /**
     * 查询产品
     *
     * @param productId 产品主键
     * @return 产品
     */
    @Override
    public Product selectProductByProductId(Long productId) {
        return getById(productId);
    }

    /**
     * 查询产品列表
     *
     * @param product 查询条件
     * @return 产品集合
     */
    @Override
    public List<Product> selectProductList(Product product) {
        return list(buildQueryWrapper(product));
    }

    /**
     * 分页查询产品列表
     *
     * @param page      分页参数
     * @param product 查询条件
     * @return 分页结果
     */
    @Override
    public Page<Product> selectProductPage(Page<Product> page, Product product) {
        return this.page(page, buildQueryWrapper(product));
    }

    /**
     * 新增产品
     *
     * @param product 产品
     * @return 是否成功
     */
    @Override
    public boolean insertProduct(Product product) {
        boolean saved = save(product);
        return saved;
    }

    /**
     * 批量新增产品
     *
     * @param products 产品列表
     * @return 成功条数
     */
    @Override
    public int batchInsertProduct(List<Product> products) {
        if (CollectionUtils.isEmpty(products)) {
            return 0;
        }
        boolean success = saveBatch(products);
        return success ? products.size() : 0;
    }

    /**
     * 修改产品
     *
     * @param product 产品
     * @return 是否成功
     */
    @Override
    public boolean updateProduct(Product product) {
        boolean updated = updateById(product);
        return updated;
    }

    /**
     * 批量删除产品
     *
     * @param productIds 主键集合
     * @return 是否成功
     */
    @Override
    public boolean deleteProductByProductIds(Long[] productIds) {
        if (productIds == null || productIds.length == 0) {
            return false;
        }
        return removeByIds(Arrays.asList(productIds));
    }

    /**
     * 删除产品信息
     *
     * @param productId 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteProductByProductId(Long productId) {
        return removeById(productId);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Product> buildQueryWrapper(Product product) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(product.getProductName()), Product::getProductName, product.getProductName());
        wrapper.eq(product.getImage() != null, Product::getImage, product.getImage());
        wrapper.eq(product.getCreateTime() != null, Product::getCreateTime, product.getCreateTime());
        wrapper.eq(product.getUpdateTime() != null, Product::getUpdateTime, product.getUpdateTime());
        return wrapper;
    }

}
