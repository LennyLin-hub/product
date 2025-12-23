package com.product.cache.utils;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * @Auther: chuan
 * @Date: 2025/12/22 - 12 - 22 - 00:26
 * @Description: spel解析器，用于动态获取方法中的签名和参数
 * @version: 1.0
 */
public class SpelParser {
    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    /** 统一使用 Spring 表达式模板风格：例如 "login:#{#username}:token" */
    private static final TemplateParserContext TEMPLATE_CONTEXT = new TemplateParserContext();

    public static String parse(String spel, Method method, Object[] args) {
        // 1. 获取方法参数名
        String[] params = discoverer.getParameterNames(method);
        // 2. 绑定上下文，在切入方法执行完后会自动销毁
        EvaluationContext context = new StandardEvaluationContext();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                context.setVariable(params[i], args[i]);
            }
        }
        // 3. 解析
        return parser.parseExpression(spel, TEMPLATE_CONTEXT).getValue(context, String.class);
    }
}
