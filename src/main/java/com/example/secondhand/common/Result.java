package com.example.secondhand.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static Result<?> fail(String msg) {
        Result<Object> r = new Result<>();
        r.code = 400;
        r.msg = msg;
        return r;
    }

    // 保留 error 名称以兼容现有代码
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 400;
        r.msg = msg;
        r.data = null;
        return r;
    }
}
