package com.seat.reserve.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：状态码透传给前端 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        log.warn("biz exception: status={} msg={}", e.getStatus(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(R.fail(e.getStatus(), e.getMessage()));
    }

    /** @Valid 校验失败 → 400，把字段名带上 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(R.fail(400, msg));
    }

    /** 路径不存在 → 404（Spring 6.1 起静态资源未命中抛的是 NoResourceFoundException） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResource(NoResourceFoundException e) {
        // 不回显用户输入的路径，避免把任意串原样写进响应体
        log.warn("no handler for: {}", e.getResourcePath());
        return ResponseEntity.status(404).body(R.fail(404, "请求的接口不存在"));
    }

    /** HTTP 方法不支持 → 405，别再落到 500 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405)
                .body(R.fail(405, "该接口不支持 " + e.getMethod() + " 方法"));
    }

    /** 权限不足 → 403（否则会被下面的 Exception 兜底成 500） */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("access denied: {}", e.getMessage());
        return ResponseEntity.status(403).body(R.fail(403, "没有权限执行该操作"));
    }

    /** 兜底：不把堆栈信息漏给前端，只写日志 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        log.error("unhandled exception", e);
        return ResponseEntity.status(500).body(R.fail(500, "服务器内部错误"));
    }

    /** 请求体 JSON 格式错误 → 400，不是 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "请求体格式错误"));
    }

    /** 查询参数类型/格式错误 → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "参数 " + e.getName() + " 格式错误"));
    }
}