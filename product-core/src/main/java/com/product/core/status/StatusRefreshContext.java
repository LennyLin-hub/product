package com.product.core.status;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 状态刷新上下文
 */
public class StatusRefreshContext {
    private final StatusEntityType entityType;
    private final List<Serializable> ids;

    public StatusRefreshContext(StatusEntityType entityType, List<Serializable> ids) {
        this.entityType = Objects.requireNonNull(entityType, "entityType must not be null");
        this.ids = ids == null ? Collections.emptyList() : List.copyOf(ids);
    }

    public static StatusRefreshContext of(StatusEntityType entityType, Serializable id) {
        return new StatusRefreshContext(entityType, id == null ? Collections.emptyList() : List.of(id));
    }

    public StatusEntityType getEntityType() {
        return entityType;
    }

    public List<Serializable> getIds() {
        return ids;
    }
}
