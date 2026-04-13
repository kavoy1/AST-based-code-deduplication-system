package com.ast.back.shared.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.ast.back.modules.auth.session.ForbiddenException;
import com.ast.back.modules.auth.session.UnauthorizedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        return Result.error(401, "未登录或登录已过期，请重新登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNoPermission(NotPermissionException e) {
        return Result.error(403, "无权限访问");
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNoRole(NotRoleException e) {
        return Result.error(403, "角色权限不足");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<?> handleUnauthorized(UnauthorizedException e) {
        return Result.error(401, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handleForbidden(ForbiddenException e) {
        return Result.error(403, e.getMessage());
    }

    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaToken(SaTokenException e) {
        return Result.error(403, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "系统运行异常，请联系管理员");
    }
}
