package com.ast.back.common;

/**
 * 自定义业务异常，用于在 Service 层抛出业务错误
 */
public class BusinessException extends RuntimeException {
    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500; // 默认业务错误状态码
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}