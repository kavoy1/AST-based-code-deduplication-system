package com.ast.back.shared.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * 设计目标：
 * 1) 对前端统一返回 {@link Result} 结构，便于页面统一提示。
 * 2) 将常见的鉴权异常映射为 401/403，便于前端做登录跳转与权限提示。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常（可预期）。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(500, e.getMessage());
    }

    /**
     * 捕获 Sa-Token 未登录异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }

    /**
     * 捕获 Sa-Token 无权限异常。
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNoPermission(NotPermissionException e) {
        return Result.error(403, "无权限访问");
    }

    /**
     * 捕获 Sa-Token 无角色异常。
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNoRole(NotRoleException e) {
        return Result.error(403, "角色权限不足");
    }

    /**
     * 捕获 Sa-Token 其它异常。
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaToken(SaTokenException e) {
        return Result.error(403, e.getMessage());
    }

    /**
     * 捕获未知异常（不应频繁出现）。
     *
     * 注意：这里打印栈信息方便开发调试，生产环境建议接入日志系统并脱敏。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "系统运行异常，请联系管理员");
    }
}