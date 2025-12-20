package com.product.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 生成业务表分页参数包装
 *
 * 说明：保持 XML 入参不变的同时，提供 IPage 能力给 MyBatis-Plus。
 */
public class GenTablePage extends GenTable implements IPage<GenTable>
{
    private static final long serialVersionUID = 1L;

    private final Page<GenTable> page;

    public GenTablePage()
    {
        this.page = new Page<>();
    }

    public GenTablePage(Page<GenTable> page)
    {
        this.page = page == null ? new Page<>() : page;
    }

    @Override
    public List<OrderItem> orders()
    {
        return page.orders();
    }

    @Override
    public List<GenTable> getRecords()
    {
        return page.getRecords();
    }

    @Override
    public IPage<GenTable> setRecords(List<GenTable> records)
    {
        page.setRecords(records);
        return this;
    }

    @Override
    public long getTotal()
    {
        return page.getTotal();
    }

    @Override
    public IPage<GenTable> setTotal(long total)
    {
        page.setTotal(total);
        return this;
    }

    @Override
    public long getSize()
    {
        return page.getSize();
    }

    @Override
    public IPage<GenTable> setSize(long size)
    {
        page.setSize(size);
        return this;
    }

    @Override
    public long getCurrent()
    {
        return page.getCurrent();
    }

    @Override
    public IPage<GenTable> setCurrent(long current)
    {
        page.setCurrent(current);
        return this;
    }
}
