package com.seat.reserve.common;

/**
 * 业务异常：自带 HTTP 状态码，由 GlobalExceptionHandler 统一转成响应。
 */
public class BizException extends RuntimeException {

    private final int status;

    public BizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    /** 抢座被占、乐观锁失败都走这个 */
    public static BizException conflict(String msg) {
        return new BizException(409, msg);
    }

    public static BizException tooManyRequests(String msg) {
        return new BizException(429, msg);
    }
}