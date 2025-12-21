package com.product.utils.http;

import jakarta.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/// 通用HTTP工具封装类
///
/// 该类提供了处理HTTP请求的常用工具方法，主要用于从ServletRequest中读取请求体内容。
/// 通常用于过滤器、拦截器等需要对请求内容进行预处理的场景。
///
///
/// @author fast
/// @version 1.0
/// @since 1.0
@Slf4j
public class HttpHelper
{
    /// 从ServletRequest中获取请求体字符串
    ///
    /// 该方法用于读取HTTP请求的Body内容，返回UTF-8编码的字符串。
    /// 注意：由于InputStream的特性，该方法只能被调用一次。如果需要多次读取请求体，
    /// 建议使用包装类（如RepeatedlyRequestWrapper）进行缓存。
    ///
    ///
    /// @param request ServletRequest对象，包含HTTP请求的所有信息
    /// @return 请求体的字符串内容，UTF-8编码。如果读取失败则返回空字符串
    /// @throws NullPointerException 如果request参数为null
    ///
    /// @see ServletRequest#getInputStream()
    /// @see java.io.BufferedReader
    /// @see java.nio.charset.StandardCharsets#UTF_8
    public static String getBodyString(ServletRequest request)
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        // 使用try-with-resources确保InputStream能够正确关闭
        try (InputStream inputStream = request.getInputStream())
        {
            // 创建带字符编码的读取器，确保中文等字符正确解析
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = "";
            // 逐行读取请求体内容
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        catch (IOException e)
        {
            // 记录警告日志，不抛出异常以保证程序继续执行
            log.warn("getBodyString出现问题！{}", e.getMessage());
        }
        finally
        {
            // 确保BufferedReader能够正确关闭
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    // 记录关闭流时的异常信息
                    log.error(ExceptionUtils.getMessage(e));
                }
            }
        }
        return sb.toString();
    }
}
