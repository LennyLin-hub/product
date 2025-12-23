package com.product.common.filter;

import com.product.common.utils.StringUtils;
import com.product.common.utils.html.EscapeUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// HTTP请求包装器 - XSS过滤处理核心类
/// 该类继承HttpServletRequestWrapper，对HTTP请求进行透明包装，
/// 在不改变原有业务逻辑的情况下，对所有请求参数和请求体进行XSS安全过滤。
/// 功能特性：
/// 1. 请求参数过滤：重写getParameterValues方法，对所有参数进行XSS清理
/// 2. JSON请求体过滤：处理application/json类型的请求体，过滤恶意脚本
/// 3. 透明过滤：对业务代码完全透明，无需修改现有接口
/// 4. 智能识别：自动识别JSON类型请求，只对JSON内容进行过滤
/// 工作原理：
/// - 重写HttpServletRequest的getParameterValues和getInputStream方法
/// - 使用EscapeUtil.clean()方法进行XSS过滤
/// - 对JSON请求体进行特殊的流处理，确保过滤后的内容能正确传递
/// 使用场景：
/// - 表单提交的参数过滤
/// - JSON API请求体过滤
/// - 富文本内容的安全处理
/// - 文件上传时的文本参数过滤
///
/// @author fast
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper
{
    /// 构造函数
    ///
    /// @param request 原始HTTP请求对象，将被包装进行XSS过滤
    public XssHttpServletRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    /// 重写请求参数获取方法，对所有参数值进行XSS过滤
    /// 该方法是XSS过滤的核心入口点，对表单提交的所有参数进行安全过滤。
    /// 处理流程：
    /// 1. 获取原始参数值数组
    /// 2. 对每个参数值使用EscapeUtil.clean()进行XSS清理
    /// 3. 去除首尾空格，提升数据质量
    /// 4. 返回过滤后的安全参数值数组
    ///
    /// @param name 请求参数名称
    /// @return XSS过滤后的参数值数组，如果参数不存在则返回null
    @Override
    public String[] getParameterValues(String name)
    {
        String[] values = super.getParameterValues(name);
        if (values != null)
        {
            int length = values.length;
            String[] escapesValues = new String[length];
            for (int i = 0; i < length; i++)
            {
                // 防xss攻击和过滤前后空格
                // EscapeUtil.clean()会移除或转义危险的HTML标签和JavaScript代码
                escapesValues[i] = EscapeUtil.clean(values[i]).trim();
            }
            return escapesValues;
        }
        return super.getParameterValues(name);
    }

    /// 重写请求输入流获取方法，对JSON请求体进行XSS过滤
    /// 该方法专门处理JSON格式的请求体，例如REST API的POST/PUT请求。
    /// 处理流程：
    /// 1. 检查请求是否为JSON格式，非JSON请求直接跳过过滤
    /// 2. 读取原始请求体内容
    /// 3. 对JSON字符串进行XSS过滤
    /// 4. 重新构建ServletInputStream供后续处理使用
    /// 技术要点：
    /// - 使用Apache Commons IOUtils读取输入流
    /// - 过滤后重建输入流，确保Spring MVC等框架能正常解析
    /// - 正确处理流的生命周期和状态管理
    ///
    /// @return XSS过滤后的ServletInputStream
    /// @throws IOException 当流操作发生异常时抛出
    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        // 非json类型，直接返回，避免对文件上传等二进制数据造成影响
        if (!isJsonRequest())
        {
            return super.getInputStream();
        }

        // 读取JSON请求体内容
        String json = IOUtils.toString(super.getInputStream(), "utf-8");

        // 为空内容，直接返回原始流
        if (StringUtils.isEmpty(json))
        {
            return super.getInputStream();
        }

        // 对JSON内容进行XSS过滤，去除潜在的恶意脚本
        json = EscapeUtil.clean(json).trim();
        byte[] jsonBytes = json.getBytes("utf-8");
        final ByteArrayInputStream bis = new ByteArrayInputStream(jsonBytes);

        // 创建新的ServletInputStream包装过滤后的内容
        return new ServletInputStream()
        {
            @Override
            public boolean isFinished()
            {
                // 对于ByteArrayInputStream，读取完毕即为完成状态
                return bis.available() == 0;
            }

            @Override
            public boolean isReady()
            {
                // ByteArrayInputStream始终就绪
                return true;
            }

            @Override
            public int available() throws IOException
            {
                return jsonBytes.length;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
                // 非异步请求，无需设置监听器
            }

            @Override
            public int read() throws IOException
            {
                return bis.read();
            }
        };
    }

    /// 判断当前请求是否为JSON格式请求
    /// 通过检查Content-Type请求头来判断请求格式，支持以下JSON类型：
    /// - application/json
    /// - application/json;charset=UTF-8
    /// - 其他以application/json开头的Content-Type
    /// 该方法用于getInputStream()方法中，只有JSON请求才会进行XSS过滤，
    /// 避免对文件上传、表单数据等其他类型请求造成不必要的影响。
    ///
    /// @return true表示是JSON请求，false表示不是JSON请求
    public boolean isJsonRequest()
    {
        String header = super.getHeader(HttpHeaders.CONTENT_TYPE);
        // 使用startsWithIgnoreCase支持带charset的Content-Type，如application/json;charset=UTF-8
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }
}
