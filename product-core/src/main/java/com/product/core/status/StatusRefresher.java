package com.product.core.status;

/**
 * 单个业务模块的状态刷新器
 */
public interface StatusRefresher {
    /**
     * 执行顺序，数字越小越下游
     */
    int getOrder();

    /**
     * 执行刷新
     */
    void refresh(StatusRefreshContext context);
}
