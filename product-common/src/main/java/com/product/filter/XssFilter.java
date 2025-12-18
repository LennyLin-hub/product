package com.product.filter;

import com.product.utils.StringUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// XSS攻击过滤器
/// 该过滤器作为XSS防护的第一道防线，对所有HTTP请求进行预处理，
/// 通过包装HttpServletRequest对象来实现对请求参数的XSS过滤。
/// 功能特性：
/// 1. 支持配置排除URL列表，对特定路径跳过XSS过滤
/// 2. 智能识别HTTP方法，对GET和DELETE等只读操作默认跳过过滤
/// 3. 将原始请求包装为XssHttpServletRequestWrapper，实现透明过滤
/// 4. 基于白名单的过滤策略，只允许安全的HTML内容通过
/// 使用场景：
/// - 防止用户输入中的恶意脚本注入
/// - 保护Web应用免受跨站脚本攻击
/// - 对用户提交的富文本内容进行安全过滤
/// 配置示例：
/// 在web.xml中配置excludes参数，排除不需要过滤的URL：
/// <init-param>
///     <param-name>excludes</param-name>
///     <param-value>/upload,/export,/api/public/*</param-value>
/// </init-param>
///
/// @author chuan
/// @since 2025/12/17
public class XssFilter implements Filter {
    /**
     * 排除链接列表
     * 存储不需要进行XSS过滤的URL模式，支持通配符匹配
     * 例如：/api/upload, /export/*, /public/**
     */
    public List<String> excludes = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 从配置中读取排除URL列表
        String tempExcludes = filterConfig.getInitParameter("excludes");
        if (StringUtils.isNotEmpty(tempExcludes))
        {
            // 按逗号分割多个URL模式
            String[] urls = tempExcludes.split(",");
            for (String url : urls)
            {
                // 去除首尾空格后添加到排除列表
                excludes.add(url.trim());
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 检查当前请求是否需要排除XSS过滤
        if (handleExcludeURL(req, resp))
        {
            // 如果需要排除，直接传递原始请求
            chain.doFilter(request, response);
            return;
        }

        // 创建XSS过滤包装器，对请求参数进行安全过滤
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(xssRequest, response);
    }

    @Override
    public void destroy() {
        // 清理资源
        Filter.super.destroy();
    }

    /// 判断请求URL是否需要排除XSS过滤
    /// 排除规则：
    /// 1. HTTP方法为GET或DELETE的只读操作默认排除
    /// 2. 请求路径匹配配置的排除列表中的任意模式
    ///
    /// @param request HTTP请求对象
    /// @param response HTTP响应对象
    /// @return true表示需要排除XSS过滤，false表示需要进行XSS过滤
    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response)
    {
        String url = request.getServletPath();
        String method = request.getMethod();

        // GET和DELETE方法为只读操作，不涉及数据修改，默认跳过XSS过滤
        if (method == null || HttpMethod.GET.matches(method) || HttpMethod.DELETE.matches(method))
        {
            return true;
        }

        // 检查请求URL是否匹配排除列表中的任一模式
        return StringUtils.matches(url, excludes);
    }
}
