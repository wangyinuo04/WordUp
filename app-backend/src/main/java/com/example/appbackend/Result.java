package com.example.appbackend;

/**
 * 全局统一 API 响应包装类
 * 所有后端接口必须返回此格式，与 Apifox 定义保持绝对一致！
 */
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // 私有构造，强制使用静态方法创建
    private Result() {}

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // --- 成功响应的快捷方法 ---
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // --- 失败响应的快捷方法 ---
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null); // 默认 500 服务器内部错误
    }

    // Getter 和 Setter (必须有，否则 Spring 无法将其转换为 JSON)
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}