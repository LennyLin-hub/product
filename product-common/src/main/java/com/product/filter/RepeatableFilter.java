package com.product.filter;

import com.product.utils.StringUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;

import java.io.IOException;

/// 请求体可重复读取过滤器
/// 该过滤器用于解决HTTP请求体只能读取一次的问题，特别是在需要多次读取JSON请求体的场景中。
/// 作为可重复请求处理的核心组件，与防重复提交机制配合使用，确保请求流的多次访问能力。
/// 解决问题：
/// 1. 流只能读取一次：ServletInputStream默认只能读取一次，再次读取会抛出异常
/// 2. 参数验证失败后的流访问：Spring Validation失败后，需要重新读取请求体进行日志记录
/// 3. 拦截器链中的多次访问：多个拦截器可能都需要访问请求体内容
/// 4. 防重复校验：需要读取请求体内容进行MD5或SHA校验
/// 功能特性：
/// 1. 智能识别：只对JSON类型的请求进行包装，避免对其他请求类型造成影响
/// 2. 透明处理：对业务代码完全透明，无需修改现有接口
/// 3. 性能优化：只对需要的请求类型进行处理，减少不必要的开销
/// 4. 兼容性好：与Spring MVC、Spring Boot等框架无缝集成
/// 工作原理：
/// - 检测Content-Type是否为application/json
/// - 将原始HttpServletRequest包装为RepeatedlyRequestWrapper
/// - 包装器会缓存请求体内容，支持多次读取
/// 使用场景：
/// - API接口的JSON参数验证
/// - 请求日志记录和审计
/// - 防重复提交校验
/// - 参数加密解密处理
/// - 请求体内容分析
/// 注意事项：
/// - 该过滤器应该配置在较高优先级，确保在其他过滤器之前执行
/// - 大文件上传请求不会被处理，避免内存溢出
/// - 包装后的请求支持标准的ServletInputStream接口
///
/// @author fast
public class RepeatableFilter implements Filter
{
    /// 过滤器初始化方法
    /// 当前过滤器无需特殊初始化配置，保留接口实现以确保符合Servlet规范。
    /// 可在未来需要时添加配置参数，如最大缓存大小、超时时间等。
    ///
    /// @param filterConfig 过滤器配置对象，包含初始化参数
    /// @throws ServletException 当初始化过程中发生错误时抛出
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // 当前版本无需特殊初始化逻辑
        // 预留扩展空间，未来可添加配置参数：
        // - maxCacheSize: 最大缓存大小限制
        // - timeout: 请求读取超时时间
        // - excludedPaths: 排除的路径列表
    }

    /// 过滤器核心处理方法
    /// 该方法实现了请求体可重复读取的核心逻辑：
    /// 1. 检查请求是否为HTTP请求且Content-Type为JSON
    /// 2. 如果是JSON请求，创建RepeatedlyRequestWrapper进行包装
    /// 3. 如果不是JSON请求或包装失败，使用原始请求
    /// 4. 将包装后的请求传递给过滤器链的下一个组件
    /// 处理流程：
    /// - 类型检查：确认是HttpServletRequest实例
    /// - 内容类型判断：检查Content-Type是否以application/json开头
    /// - 请求包装：创建可重复读取的包装器
    /// - 链式传递：将处理后的请求传递给后续过滤器
    ///
    /// @param request Servlet请求对象
    /// @param response Servlet响应对象
    /// @param chain 过滤器链，用于传递请求到下一个过滤器
    /// @throws IOException 当I/O操作发生异常时抛出
    /// @throws ServletException 当Servlet处理发生异常时抛出
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        ServletRequest requestWrapper = null;

        // 检查请求是否为HTTP请求且Content-Type为JSON格式
        if (request instanceof HttpServletRequest
                && StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE))
        {
            // 创建可重复读取的请求包装器
            // 包装器会缓存请求体内容，支持多次读取而不影响原始流
            requestWrapper = new RepeatedlyRequestWrapper((HttpServletRequest) request, response);
        }

        // 根据包装结果决定传递原始请求还是包装后的请求
        if (null == requestWrapper)
        {
            // 非JSON请求或包装失败，直接传递原始请求
            chain.doFilter(request, response);
        }
        else
        {
            // JSON请求，传递包装后的可重复读取请求
            chain.doFilter(requestWrapper, response);
        }
    }

    /// 过滤器销毁方法
    /// 过滤器生命周期结束时调用，用于清理资源。
    /// 当前实现无需特殊清理逻辑，但保留接口实现以符合Servlet规范。
    /// 未来扩展可能包括：
    /// - 清理缓存的请求体数据
    /// - 关闭打开的资源连接
    /// - 记录过滤器使用统计信息
    @Override
    public void destroy()
    {
        // 当前版本无需特殊清理逻辑
        // 预留扩展空间，未来可添加：
        // - 缓存清理逻辑
        // - 统计信息输出
        // - 资源释放操作
    }
}
