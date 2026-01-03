package com.product.framework.manager.status;

import com.product.core.status.StatusEntityType;
import com.product.core.status.StatusRefreshContext;
import com.product.core.status.StatusRefreshService;
import com.product.core.status.StatusRefresher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 状态刷新服务实现，按下游 -> 上游顺序执行
 */
@Service
public class StatusRefreshServiceImpl implements StatusRefreshService {
    private final List<StatusRefresher> refreshers;

    @Autowired
    public StatusRefreshServiceImpl(List<StatusRefresher> refreshers) {
        this.refreshers = refreshers;
    }

    @Override
    public void refresh(StatusRefreshContext context) {
        if (context == null || refreshers == null || refreshers.isEmpty()) {
            return;
        }
        StatusEntityType startType = context.getEntityType();
        int startOrder = startType == null ? Integer.MIN_VALUE : startType.getOrder();
        refreshers.stream()
                .sorted(Comparator.comparingInt(StatusRefresher::getOrder))
                .filter(refresher -> refresher.getOrder() >= startOrder)
                .forEach(refresher -> refresher.refresh(context));
    }
}
