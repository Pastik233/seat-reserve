package com.seat.reserve.security;

/**
 * 存进 Redis 的权限快照（JSON）。只放"每次请求都可能用到"的字段，不放密码。
 */
public record UserSnapshot(
        Long id,
        String username,
        String nickname,
        String roleCode,
        /** 拉黑截止时间，null 表示未拉黑；存字符串避免日期序列化差异 */
        String blacklistUntil
) {
}