package com.product.properties;

import com.product.annotation.Anonymous;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
import java.util.regex.Pattern;

/// 允许匿名访问URL配置属性类
/// 该类是一个Spring配置类，用于自动收集和管理所有标注了@Anonymous注解的URL路径。
/// 在Spring Security配置中，这些URL将被允许匿名访问，无需用户认证。
/// 核心功能：
/// 1. 自动扫描：在应用启动时扫描所有Controller类和方法
/// 2. 注解识别：识别@Anonymous注解标注的URL路径
/// 3. 路径处理：将路径变量替换为通配符，便于匹配
/// 4. 配置集成：与Spring Security配置无缝集成
/// 5. 动态更新：支持运行时获取和更新允许访问的URL列表
/// 工作原理：
/// 1. 实现InitializingBean接口，在Bean初始化完成后执行URL扫描
/// 2. 实现ApplicationContextAware接口，获取Spring应用上下文
/// 3. 通过RequestMappingHandlerMapping获取所有Controller映射信息
/// 4. 检查每个映射的Controller类和方法是否标注@Anonymous注解
/// 5. 提取URL路径，将路径变量（如{id}）替换为通配符(*)
/// 6. 将处理后的URL路径存储到列表中供Security配置使用
/// 路径处理规则：
/// - /api/user/{id} → /api/user/\*
/// 注解支持：
/// - 类级别注解：整个Controller的所有方法都允许匿名访问
/// - 方法级别注解：仅特定方法允许匿名访问
/// - 优先级：方法级别注解优先于类级别注解
/// 使用场景：
/// - 登录注册接口：用户未登录时需要访问的接口
/// - 公共API接口：开放给外部系统调用的接口
/// - 资源下载接口：无需认证即可访问的静态资源
/// - 健康检查接口：监控系统使用的接口
/// 安全考虑：
/// - 谨慎使用匿名访问，避免泄露敏感信息
/// - 定期审查允许匿名访问的URL列表
/// - 考虑对匿名访问接口增加频率限制
/// - 记录匿名访问的操作日志
///
/// @author fast
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    /**
     * 路径变量匹配正则表达式
     * 用于匹配URL中的路径变量，如{id}、{categoryId}等
     * 模式：\\{(.*?)\\} - 匹配花括号中的任意内容
     */
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    /**
     * Spring应用上下文
     * 用于获取Spring容器中的Bean，特别是RequestMappingHandlerMapping
     */
    private ApplicationContext applicationContext;

    /**
     * 允许匿名访问的URL路径列表
     * 存储所有经过处理的URL路径，格式为通配符匹配
     */
    private List<String> urls = new ArrayList<>();

    /**
     * 通配符常量
     * 用于替换URL路径变量，支持多级路径匹配
     */
    public String ASTERISK = "*";

    /// Bean初始化后的回调方法
    /// 该方法在Spring容器完成Bean初始化后自动调用，用于扫描和收集
    /// 所有标注了@Anonymous注解的URL路径。是整个URL收集功能的核心入口。
    /// 处理流程：
    /// 1. 获取RequestMappingHandlerMapping Bean
    /// 2. 获取所有Controller方法映射信息
    /// 3. 遍历每个映射，检查注解情况
    /// 4. 提取并处理URL路径
    /// 5. 将处理后的路径添加到允许访问列表
    /// 注解处理逻辑：
    /// - 优先检查方法级别的@Anonymous注解
    /// - 然后检查类级别的@Anonymous注解
    /// - 任一位置有注解即标记为允许匿名访问
    ///
    /// @throws Exception 初始化过程中可能抛出的异常
    @Override
    public void afterPropertiesSet()
    {
        // 获取Spring MVC的请求映射处理器
        RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);

        // 获取所有Controller方法的映射信息
        // Map的Key是RequestMappingInfo，Value是HandlerMethod
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        // 遍历所有映射信息
        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 检查方法级别是否有@Anonymous注解
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous ->
                Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues()) //
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));

            // 检查类级别是否有@Anonymous注解
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous ->
                Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
        });
    }

    /// 设置Spring应用上下文
    /// 实现ApplicationContextAware接口，Spring容器会自动调用此方法
    /// 注入ApplicationContext实例，用于后续获取其他Bean。
    ///
    /// @param context Spring应用上下文对象
    /// @throws BeansException 注入过程中可能抛出的异常
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;
    }

    /// 获取允许匿名访问的URL路径列表
    /// 返回经过处理的所有URL路径，这些路径已经将路径变量
    /// 替换为通配符，可以直接用于Spring Security的配置中。
    ///
    /// @return URL路径列表，每个路径都使用通配符格式
    public List<String> getUrls()
    {
        return urls;
    }

    /// 设置允许匿名访问的URL路径列表
    /// 允许外部程序动态设置允许匿名访问的URL路径，
    /// 主要用于测试或特殊配置场景。
    ///
    /// @param urls URL路径列表
    public void setUrls(List<String> urls)
    {
        this.urls = urls;
    }
}
