package com.product.framework.manager.status;

import com.product.core.status.StatusRefreshContext;
import com.product.core.status.StatusRefreshId;
import com.product.core.status.StatusRefreshService;
import com.product.core.status.StatusRefreshTrigger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 基于注解触发状态刷新
 */
@Aspect
@Component
public class StatusRefreshAspect {
    private final StatusRefreshService statusRefreshService;

    @Autowired
    public StatusRefreshAspect(StatusRefreshService statusRefreshService) {
        this.statusRefreshService = statusRefreshService;
    }

    @AfterReturning(pointcut = "@annotation(trigger)")
    public void afterReturning(JoinPoint joinPoint, StatusRefreshTrigger trigger) {
        // 获取id
        Serializable id = extractId(joinPoint);
        if (id == null) {
            return;
        }
        statusRefreshService.refresh(StatusRefreshContext.of(trigger.startType(), id));
    }

    private Serializable extractId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType() == StatusRefreshId.class) {
                    Object value = args[i];
                    return value instanceof Serializable ? (Serializable) value : null;
                }
            }
        }
        return null;
    }
}
