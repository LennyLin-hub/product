package com.product.framework.interceptor.impl;

import com.alibaba.fastjson2.JSON;
import com.product.common.annotation.RepeatSubmit;
import com.product.common.constant.CacheConstants;
import com.product.common.core.redis.RedisCache;
import com.product.framework.filter.RepeatedlyRequestWrapper;
import com.product.framework.interceptor.RepeatSubmitInterceptor;
import com.product.common.utils.StringUtils;
import com.product.common.utils.http.HttpHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/// 相同URL和参数防重复提交拦截器实现
/// 该拦截器通过比较请求URL、请求参数和请求时间来判断是否为重复提交。
/// 基于Redis实现分布式环境下的防重复提交功能，确保在集群环境中也能正常工作。
/// 核心原理：
/// 1. 请求指纹：基于URL + 请求参数 + 用户标识生成唯一的请求指纹
/// 2. 时间窗口：在指定时间间隔内的相同请求被视为重复提交
/// 3. Redis存储：使用Redis存储请求历史，支持分布式部署
/// 4. 双重检查：同时检查参数内容和时间间隔
/// 防重复策略：
/// - URL相同：请求路径必须完全一致
/// - 参数相同：请求体或URL参数必须完全一致
/// - 时间限制：两次请求间隔必须小于指定的时间阈值
/// - 用户隔离：不同用户的请求不相互影响
/// 技术特性：
/// 1. 分布式支持：基于Redis实现，支持多服务器部署
/// 2. 灵活配置：支持自定义时间间隔和错误消息
/// 3. 参数兼容：支持JSON请求体和URL参数两种格式
/// 4. 性能优化：使用高效的Redis操作，减少数据库压力
/// 5. 内存安全：自动过期机制，避免Redis内存泄漏
/// 数据结构：
/// Redis Key: REPEAT_SUBMIT_KEY + {url} + {userToken}
/// Redis Value:
/// {
///   "{url}": {
///     "repeatParams": "{JSON格式的请求参数}",
///     "repeatTime": {时间戳}
///   }
/// }
/// Redis TTL: 根据@RepeatSubmit注解的interval参数设置
/// 适用场景：
/// - 表单重复提交：用户多次点击提交按钮
/// - API重复调用：客户端因网络问题重试请求
/// - 支付重复请求：防止重复支付或订单创建
/// - 数据重复操作：防止重复的数据插入或更新
/// 使用示例：
/// <pre>
/// // Controller方法使用
/// &#64;PostMapping("/save")
/// &#64;RepeatSubmit(interval = 5000, message = "请不要重复提交")
/// public AjaxResult save(@RequestBody UserDTO user) {
///     return userService.save(user);
/// }
/// </pre>
/// 注意事项：
/// - 依赖Redis服务，确保Redis可用性
/// - 参数完全匹配，包括参数顺序和格式
/// - 时间间隔根据业务需求合理设置
/// - 建议与前端防重复按钮配合使用
///
/// @author fast
@Component
public class SameUrlDataInterceptor extends RepeatSubmitInterceptor
{
    /**
     * 请求参数缓存键常量
     * 用于在Redis缓存中存储请求参数的键名
     */
    public final String REPEAT_PARAMS = "repeatParams";

    /**
     * 请求时间缓存键常量
     * 用于在Redis缓存中存储请求时间的键名
     */
    public final String REPEAT_TIME = "repeatTime";

    /**
     * 用户Token请求头名称
     * 从配置文件中读取，用于识别不同用户的请求
     */
    @Value("${token.header}")
    private String header;

    /**
     * Redis缓存操作工具
     * 用于存储和查询请求历史记录
     */
    @Autowired
    private RedisCache redisCache;

    /// 防重复提交验证的核心实现方法
    /// 该方法实现了基于URL、参数和时间的防重复提交逻辑：
    /// 1. 提取当前请求的参数（支持JSON和表单两种格式）
    /// 2. 构建请求指纹（URL + 参数 + 用户标识）
    /// 3. 查询Redis中的历史请求记录
    /// 4. 比较参数和时间戳判断是否重复
    /// 5. 存储当前请求记录供后续比较使用
    /// 处理流程：
    /// - 参数提取：优先从请求体获取JSON参数，其次从URL参数获取
    /// - 指纹生成：URL + 用户Token作为唯一标识
    /// - 历史查询：从Redis获取上一次相同请求的记录
    /// - 双重验证：参数内容和时间间隔都满足条件才认定为重复
    /// - 缓存更新：将当前请求记录存储到Redis中
    ///
    /// @param request 当前HTTP请求对象
    /// @param annotation @RepeatSubmit注解实例，包含时间间隔等配置
    /// @return true表示是重复提交，false表示不是重复提交
    @SuppressWarnings("unchecked")
    @Override
    public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation)
    {
        String nowParams = "";

        // 尝试从请求体中获取JSON参数
        // 需要配合RepeatedlyRequestWrapper使用，支持多次读取请求体
        if (request instanceof RepeatedlyRequestWrapper)
        {
            RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
            nowParams = HttpHelper.getBodyString(repeatedlyRequest);
        }

        // 如果请求体为空，则获取URL参数
        // 兼容GET请求和表单提交的场景
        if (StringUtils.isEmpty(nowParams))
        {
            nowParams = JSON.toJSONString(request.getParameterMap());
        }

        // 构建当前请求数据映射
        Map<String, Object> nowDataMap = new HashMap<String, Object>();
        nowDataMap.put(REPEAT_PARAMS, nowParams);
        nowDataMap.put(REPEAT_TIME, System.currentTimeMillis());

        // 获取请求URL作为缓存键的一部分
        String url = request.getRequestURI();

        // 获取用户Token作为唯一标识
        // 如果没有Token则使用空字符串，实现用户级别的隔离
        String submitKey = StringUtils.trimToEmpty(request.getHeader(header));

        // 构建Redis缓存键：基础键 + URL + 用户Token
        String cacheRepeatKey = CacheConstants.REPEAT_SUBMIT_KEY + url + submitKey;

        // 从Redis中获取历史请求记录
        Object sessionObj = redisCache.getCacheObject(cacheRepeatKey);
        if (sessionObj != null)
        {
            Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
            if (sessionMap.containsKey(url))
            {
                // 获取上一次请求的记录
                Map<String, Object> preDataMap = (Map<String, Object>) sessionMap.get(url);

                // 双重检查：参数内容相同 + 时间间隔在限制范围内
                if (compareParams(nowDataMap, preDataMap) && compareTime(nowDataMap, preDataMap, annotation.interval()))
                {
                    return true; // 是重复提交
                }
            }
        }

        // 存储当前请求记录，设置过期时间为注解中配置的时间间隔
        Map<String, Object> cacheMap = new HashMap<String, Object>();
        cacheMap.put(url, nowDataMap);
        redisCache.setCacheObject(cacheRepeatKey, cacheMap, annotation.interval(), TimeUnit.MILLISECONDS);

        return false; // 不是重复提交
    }

    /// 比较两次请求的参数是否相同
    /// 通过字符串精确匹配来判断参数内容是否一致。
    /// 这种方式要求参数的格式、顺序都必须完全相同。
    ///
    /// @param nowMap 当前请求数据映射
    /// @param preMap 上一次请求数据映射
    /// @return true表示参数相同，false表示参数不同
    private boolean compareParams(Map<String, Object> nowMap, Map<String, Object> preMap)
    {
        String nowParams = (String) nowMap.get(REPEAT_PARAMS);
        String preParams = (String) preMap.get(REPEAT_PARAMS);
        return nowParams.equals(preParams);
    }

    /// 比较两次请求的时间间隔是否在指定范围内
    /// 通过计算两次请求的时间差，判断是否小于配置的时间间隔。
    /// 如果时间差小于指定间隔，则认为是重复提交。
    ///
    /// @param nowMap 当前请求数据映射
    /// @param preMap 上一次请求数据映射
    /// @param interval 允许的最小时间间隔（毫秒）
    /// @return true表示时间间隔小于指定值（重复），false表示时间间隔足够（不重复）
    private boolean compareTime(Map<String, Object> nowMap, Map<String, Object> preMap, int interval)
    {
        long time1 = (Long) nowMap.get(REPEAT_TIME);
        long time2 = (Long) preMap.get(REPEAT_TIME);
        if ((time1 - time2) < interval)
        {
            return true; // 时间间隔过短，是重复提交
        }
        return false; // 时间间隔足够，不是重复提交
    }
}

