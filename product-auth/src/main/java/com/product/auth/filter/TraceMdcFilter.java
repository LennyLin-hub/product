package com.product.auth.filter;

import com.product.common.utils.StringUtils;
import com.product.common.utils.ip.IpUtils;
import com.product.common.utils.uuid.IdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个请求注入统一的日志链路字段。
 */
@Component
public class TraceMdcFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveOrGenerateId(request.getHeader(TRACE_ID_HEADER));
        String requestId = resolveOrGenerateId(request.getHeader(REQUEST_ID_HEADER));
        try {
            // MDC.put()存入线程上下文
            // 全链路追踪id
            MDC.put("traceId", traceId);
            // 单词请求id
            MDC.put("requestId", requestId);
            // 客户端ip
            MDC.put("clientIp", IpUtils.getIpAddr(request));
            // 请求方法
            MDC.put("httpMethod", request.getMethod());
            // 请求路径
            MDC.put("requestUri", request.getRequestURI());
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            // 清理线程
            MDC.clear();
        }
    }

    private String resolveOrGenerateId(String headerValue) {
        if (StringUtils.isNotEmpty(headerValue)) {
            return headerValue.trim();
        }
        return IdUtils.simpleUUID();
    }
}
