package com.ast.back.common;

import lombok.Data;

/**
 * 统一结果返回类
 * @param <T> 响应数据的类型
 */
@Data
public class Result <T>{
    private Integer code;    // 状态码：200-成功，500-服务器错误，403-无权限
    private String msg;      // 提示信息
    private T data;          // 后端返回给前端的具体数据（用户信息、查重报告等）

    // 快捷成功方法 - 无数据返回
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    // 快捷成功方法 - 有数据返回
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 快捷失败方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    // 自定义状态码失败方法（比如 403 无权限）
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
