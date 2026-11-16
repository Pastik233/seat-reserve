package com.seat.reserve.common;

/**
 * 统一返回体：code=0 表示成功；失败时 code 与 HTTP 状态码一致（400/403/404/409/429/500）。
 */
public record R<T>(int code, String msg, T data) {

    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data);
    }

    public static <T> R<T> ok() {
        return new R<>(0, "ok", null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}