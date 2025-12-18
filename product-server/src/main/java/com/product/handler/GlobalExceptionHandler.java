package com.product.handler;

import com.product.constant.HttpStatus;
import com.product.core.text.Convert;
import com.product.entity.result.AjaxResult;
import com.product.exception.ServiceException;
import com.product.utils.StringUtils;
import com.product.utils.html.EscapeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

/**
 * 全局异常处理器
 *
 * @author fast
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /**
     * sql异常处理 - 增强版
     */
    @ExceptionHandler({
            SQLException.class,
            org.springframework.dao.DataAccessException.class,
            org.apache.ibatis.exceptions.PersistenceException.class,
            org.mybatis.spring.MyBatisSystemException.class
    })
    public AjaxResult handleSqlException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生SQL异常", requestURI, e);

        // 提取根本原因
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = getSqlErrorMessage(cause);
        return AjaxResult.error(message);
    }

    private String getSqlErrorMessage(Throwable e) {
        if (e == null) {
            return "数据库操作异常，请联系管理员";
        }

        String errorMsg = e.getMessage();
        if (StringUtils.isEmpty(errorMsg)) {
            return "数据库操作异常，请联系管理员";
        }

        // 根据具体错误类型返回友好提示
        if (errorMsg.contains("Duplicate entry")) {
            return "数据已存在，请检查重复数据";
        } else if (errorMsg.contains("foreign key constraint")) {
            return "数据关联约束异常，请检查相关数据";
        } else if (errorMsg.contains("cannot be null")) {
            return "必填字段不能为空";
        } else if (errorMsg.contains("Data too long")) {
            return "输入数据过长，请检查字段长度";
        } else if (errorMsg.contains("Incorrect string value")) {
            return "数据格式错误，请检查编码格式";
        } else {
            return "数据库操作异常：" + errorMsg;
        }
    }


    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }
}
