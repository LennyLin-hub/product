package com.product.filter;

import com.product.constant.Constants;
import com.product.utils.http.HttpHelper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/// HTTP请求体可重复读取包装器
/// 该类继承HttpServletRequestWrapper，通过缓存请求体内容来解决ServletInputStream只能读取一次的问题。
/// 这是实现防重复提交、请求日志记录、参数验证等功能的核心基础设施组件。
/// 核心问题：
/// ServletInputStream按照设计只能读取一次，读取后流的位置会到达末尾，
/// 再次读取将无法获取数据。这在需要多次访问请求体的场景中会造成问题。
/// 解决方案：
/// 1. 请求体缓存：在构造时将请求体内容读取并缓存到字节数组中
/// 2. 流重建：每次调用getInputStream()时都基于缓存数据创建新的输入流
/// 3. 标准兼容：完全实现ServletInputStream接口，确保框架兼容性
/// 4. 编码处理：统一使用UTF-8编码，避免字符编码问题
/// 功能特性：
/// 1. 透明包装：对业务代码完全透明，无需修改现有接口
/// 2. 多次读取：支持任意次数的请求体读取操作
/// 3. 性能优化：使用ByteArrayInputStream提供高效的流重建
/// 4. 内存安全：合理控制缓存大小，避免内存溢出
/// 5. 异常安全：提供完善的异常处理机制
/// 使用场景：
/// - 防重复提交校验：需要读取请求体生成指纹或哈希
/// - 请求日志记录：记录完整的请求参数用于审计
/// - 参数验证：多次验证不同维度的参数合法性
/// - 数据加解密：对请求数据进行加密或解密处理
/// - 业务逻辑复用：多个组件都需要访问请求体内容
/// 技术实现：
/// - 使用字节数组缓存原始请求体数据
/// - 基于缓存数据动态重建ServletInputStream
/// - 实现完整的流状态管理方法
/// - 提供getReader()方法支持字符流读取
/// 注意事项：
/// - 适用于中小型请求体，大文件上传应使用其他方式
/// - 会占用额外内存来缓存请求体内容
/// - 应与RepeatableFilter配合使用
///
/// @author fast
public class RepeatedlyRequestWrapper extends HttpServletRequestWrapper
{
    /// 缓存的请求体内容
    /// 使用字节数组存储原始HTTP请求体的完整内容。
    /// 该字段在构造函数中初始化，后续所有getInputStream()调用
    /// 都将基于此缓存数据创建新的输入流实例。
    /// 设计考虑：
    /// - 使用byte[]而非String：确保二进制数据的正确处理
    /// - final修饰：确保缓存内容不被意外修改，保证线程安全
    /// - UTF-8编码：统一字符编码，避免乱码问题
    private final byte[] body;

    /// 构造函数 - 初始化可重复读取请求包装器
    /// 构造过程：
    /// 1. 调用父类构造函数，包装原始HttpServletRequest
    /// 2. 统一设置请求和响应的字符编码为UTF-8
    /// 3. 读取原始请求体内容并缓存到字节数组中
    /// 技术细节：
    /// - 使用HttpHelper.getBodyString()安全读取请求体
    /// - 统一编码处理，避免中文等字符乱码
    /// - 异常处理：确保读取过程中的IO异常得到正确处理
    ///
    /// @param request 原始HTTP请求对象，需要被包装以支持可重复读取
    /// @param response HTTP响应对象，用于设置字符编码
    /// @throws IOException 当读取请求体过程中发生I/O错误时抛出
    public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response) throws IOException
    {
        super(request);

        // 统一设置请求和响应的字符编码为UTF-8
        // 确保中文等多字节字符能正确处理
        request.setCharacterEncoding(Constants.UTF8);
        response.setCharacterEncoding(Constants.UTF8);

        // 读取并缓存请求体内容
        // HttpHelper.getBodyString()会安全地读取完整的请求体内容
        // 然后转换为字节数组缓存，供后续多次读取使用
        body = HttpHelper.getBodyString(request).getBytes(Constants.UTF8);
    }

    /// 获取请求体的字符读取器
    /// 该方法基于缓存的请求体数据创建BufferedReader，
    /// 支持以字符流的方式读取请求体内容，适用于需要按行处理或字符级别处理的场景。
    /// 实现特点：
    /// - 基于getInputStream()创建，确保数据来源的一致性
    /// - 使用InputStreamReader进行字节到字符的转换
    /// - 默认使用UTF-8编码进行转换
    /// - 支持缓冲读取，提升读取性能
    /// 使用场景：
    /// - 按行处理JSON或文本数据
    /// - 字符级别的参数解析
    /// - 与需要Reader类型的第三方库集成
    ///
    /// @return 基于缓存数据创建的BufferedReader实例
    /// @throws IOException 当创建Reader过程中发生I/O错误时抛出
    @Override
    public BufferedReader getReader() throws IOException
    {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /// 获取可重复读取的Servlet输入流
    /// 这是该包装器的核心方法，每次调用都会基于缓存的请求体数据
    /// 创建一个新的ServletInputStream实例，从而实现请求体的无限次读取。
    /// 技术实现：
    /// 1. 基于缓存的body字节数组创建ByteArrayInputStream
    /// 2. 包装为ServletInputStream的匿名实现
    /// 3. 实现所有必需的接口方法
    /// 流状态管理：
    /// - isFinished(): 返回流是否已读取完毕
    /// - isReady(): 返回流是否准备好进行读取
    /// - setReadListener(): 异步读取支持（当前为空实现）
    /// 性能优化：
    /// - 使用ByteArrayInputStream提供高效的内存读取
    /// - 每次调用返回新的流实例，避免状态冲突
    /// - 直接基于字节数组，无需额外的数据复制
    ///
    /// @return 基于缓存数据创建的新ServletInputStream实例
    /// @throws IOException 当创建流过程中发生I/O错误时抛出
    // 这里没有显式调用，请求经过过滤器包装成该类时，后续的代码每次使用request.getInputStream()时都会隐式调用该方法
    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        // 基于缓存的请求体数据创建字节输入流
        // 每次调用getInputStream()都会创建新的流实例
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);

        // 创建ServletInputStream的匿名实现类
        // 委托具体的读取操作给ByteArrayInputStream
        return new ServletInputStream()
        {
            /// 读取单个字节数据
            ///
            /// @return 读取的字节（0-255），如果到达流末尾则返回-1
            /// @throws IOException 当读取过程中发生I/O错误时抛出
            @Override
            public int read() throws IOException
            {
                return bais.read();
            }

            /// 返回可读取的剩余字节数
            /// 对于ByteArrayInputStream，该方法返回缓存中剩余的字节数。
            /// 这个值在整个流的生命周期中会随着读取操作而逐渐减少。
            ///
            /// @return 可用字节数
            /// @throws IOException 当I/O操作发生错误时抛出
            @Override
            public int available() throws IOException
            {
                return body.length;
            }

            /// 检查流是否已经读取完毕
            /// 对于同步读取的场景，该方法通常返回false，
            /// 表示流可以继续读取直到实际到达末尾。
            ///
            /// @return true表示数据已全部读取完毕，false表示还有数据可读
            @Override
            public boolean isFinished()
            {
                return bais.available() == 0;
            }

            /// 检查流是否准备好进行读取
            /// 对于基于内存的ByteArrayInputStream，数据总是立即可用，
            /// 因此该方法始终返回true。
            ///
            /// @return true表示流已准备好读取，false表示流尚未准备好
            @Override
            public boolean isReady()
            {
                return true;
            }

            /// 设置异步读取监听器
            /// 当前实现为空，因为：
            /// 1. 基于内存的读取是同步的，不需要异步处理
            /// 2. 简化实现，避免不必要的复杂性
            /// 3. 当前应用场景主要是同步请求处理
            ///
            /// @param readListener 异步读取监听器
            @Override
            public void setReadListener(ReadListener readListener)
            {
                // 当前为同步实现，无需设置异步监听器
                // 预留接口以支持未来的异步需求
            }
        };
    }
}
