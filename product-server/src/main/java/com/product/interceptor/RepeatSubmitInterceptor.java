package com.product.interceptor;

import com.alibaba.fastjson2.JSON;
import com.product.annotation.RepeatSubmit;
import com.product.entity.result.AjaxResult;
import com.product.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 防止重复提交拦截器抽象基类
 *
 * 该拦截器作为防重复提交功能的核心组件，通过AOP的方式在方法执行前进行重复性检查。
 * 采用模板方法设计模式，定义了防重复提交的标准流程，具体验证逻辑由子类实现。
 *
 * 核心问题：
 * 1. 用户快速点击：用户在短时间内多次点击提交按钮
 * 2. 网络重试：网络延迟导致的客户端重试请求
 * 3. 恶意攻击：恶意用户通过脚本进行重复请求
 * 4. 表单重复：浏览器刷新或后退导致的重复提交
 *
 * 解决方案：
 * 1. 注解驱动：通过@RepeatSubmit注解标记需要防护的方法
 * 2. 拦截器模式：在方法执行前进行重复性检查
 * 3. 模板方法：定义标准流程，具体策略由子类实现
 * 4. 统一响应：重复提交时返回标准化的错误响应
 *
 * 设计模式：
 * - 模板方法模式：定义防重复提交的流程骨架，具体实现由子类完成
 * - 策略模式：不同的子类实现不同的防重复策略
 * - 责任链模式：作为Spring MVC拦截器链中的一环
 *
 * 功能特性：
 * 1. 声明式配置：通过注解即可启用防重复功能
 * 2. 统一拦截：所有标注@RepeatSubmit的方法都会被拦截
 * 3. 灵活策略：支持多种防重复提交的具体实现策略
 * 4. 标准响应：重复提交时返回统一的JSON错误格式
 * 5. 非侵入性：对业务代码完全透明，无需修改原有逻辑
 *
 * 工作流程：
 * 1. 拦截请求：Spring MVC在方法执行前调用preHandle方法
 * 2. 注解检查：检查目标方法是否标注了@RepeatSubmit注解
 * 3. 重复验证：调用子类实现的具体验证逻辑
 * 4. 结果处理：如果是重复提交，返回错误响应并拦截请求
 * 5. 正常执行：如果不是重复提交，允许正常执行业务方法
 *
 * 使用场景：
 * - 表单提交：防止用户重复点击提交按钮
 * - 支付接口：防止重复支付或订单重复创建
 * - 数据创建：防止重复创建相同的数据记录
 * - 状态变更：防止重复的状态转换操作
 *
 * 扩展点：
 * - 时间窗口：可自定义防重复的时间间隔
 * - 标识生成：可自定义重复请求的标识生成策略
 * - 存储方式：可自定义重复状态的存储方式（Redis、数据库等）
 * - 错误处理：可自定义重复提交时的错误响应格式
 *
 * @author fast
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor
{
    /**
     * 前置拦截方法 - 防重复提交的核心入口
     *
     * 该方法在目标Controller方法执行前被调用，负责检查是否需要进行防重复提交验证。
     * 采用模板方法模式，定义了标准的防重复提交流程。
     *
     * 处理逻辑：
     * 1. 类型检查：确认处理器是HandlerMethod类型（Controller方法）
     * 2. 注解扫描：检查目标方法是否标注了@RepeatSubmit注解
     * 3. 重复验证：如果需要防重复，调用子类的具体验证逻辑
     * 4. 结果处理：根据验证结果决定是否拦截请求
     *
     * 异常处理：
     * - 方法反射异常：获取注解或方法时可能抛出的异常
     * - 验证逻辑异常：子类实现中可能抛出的业务异常
     * - 响应写入异常：向客户端写入错误响应时的IO异常
     *
     * @param request 当前HTTP请求对象，包含请求参数、会话等信息
     * @param response 当前HTTP响应对象，用于写入错误响应
     * @param handler 处理器对象，通常是Controller方法的包装
     * @return true表示允许继续执行，false表示拦截请求（重复提交）
     * @throws Exception 当处理过程中发生任何异常时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        // 检查处理器是否为HandlerMethod类型（即Controller方法）
        // 只有Controller方法才需要进行防重复提交检查
        if (handler instanceof HandlerMethod)
        {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();

            // 检查目标方法是否标注了@RepeatSubmit注解
            // 只有标注了该注解的方法才需要进行防重复提交检查
            RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);
            if (annotation != null)
            {
                // 调用子类实现的具体重复提交验证逻辑
                // 子类可以实现基于时间间隔、请求参数、用户会话等不同策略
                if (this.isRepeatSubmit(request, annotation))
                {
                    // 如果是重复提交，构建错误响应并返回
                    AjaxResult ajaxResult = AjaxResult.error(annotation.message());

                    // 将错误响应写入HTTP响应流
                    // 使用FastJSON将响应对象序列化为JSON字符串
                    ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));

                    // 返回false拦截请求，阻止目标方法执行
                    return false;
                }
            }

            // 没有标注@RepeatSubmit注解或验证通过，允许继续执行
            return true;
        }
        else
        {
            // 非HandlerMethod类型的处理器（如静态资源处理器），直接放行
            // 这些类型的请求通常不需要进行防重复提交检查
            return true;
        }
    }

    /**
     * 验证是否重复提交的抽象方法
     *
     * 该方法定义了防重复提交验证的接口，具体的验证策略由子类实现。
     * 通过模板方法模式，允许不同的子类实现不同的防重复策略。
     *
     * 设计思想：
     * - 模板方法模式：父类定义流程骨架，子类实现具体步骤
     * - 策略模式：不同的子类可以采用不同的验证策略
     * - 开闭原则：对扩展开放，对修改封闭
     *
     * 常见实现策略：
     * 1. 时间窗口策略：基于时间间隔的防重复（如：5秒内不允许重复）
     * 2. 请求指纹策略：基于请求参数生成指纹，相同指纹视为重复
     * 3. 会话策略：基于用户会话进行防重复
     * 4. 令牌策略：基于一次性令牌进行防重复
     * 5. 组合策略：多种策略的组合使用
     *
     * 实现要点：
     * - 线程安全：考虑到并发请求的情况
     * - 性能优化：选择高效的存储和查询方式
     * - 异常处理：妥善处理各种异常情况
     * - 内存管理：避免存储过多历史数据造成内存泄漏
     *
     * @param request 当前HTTP请求对象，包含请求参数、头信息等
     * @param annotation @RepeatSubmit注解实例，包含配置参数（时间间隔、错误消息等）
     * @return true表示是重复提交，false表示不是重复提交
     * @throws Exception 当验证过程中发生异常时抛出（如网络异常、存储异常等）
     */
    public abstract boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation);
}
