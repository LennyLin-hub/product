package com.product.config;

import com.product.common.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 服务相关配置
 *
 * @author fast
 */
@Component
public class ServerConfig
{
    /**
     * 获取完整的请求路径，包括：域名，端口，上下文访问路径
     *
     * @return 服务地址
     */
    public String getUrl()
    {
        // 从当前线程绑定的请求中获取完整访问域名（含端口与上下文）
        HttpServletRequest request = ServletUtils.getRequest();
        return getDomain(request);
    }

    public static String getDomain(HttpServletRequest request)
    {
        // 去掉 URI 部分，只保留协议 + 域名 + 端口 + contextPath 作为基础地址
        StringBuffer url = request.getRequestURL();
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }
}
