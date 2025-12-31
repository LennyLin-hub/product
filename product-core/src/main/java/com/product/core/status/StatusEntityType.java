package com.product.core.status;

/**
 * 状态刷新起点与执行顺序（数字越小越下游）
 */
public enum StatusEntityType {
    PRODUCTION_BATCH(1),
    ORDER_LINE(2),
    CUSTOMER_ORDER(3);

    private final int order;

    StatusEntityType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
