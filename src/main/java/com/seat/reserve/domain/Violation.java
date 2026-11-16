package com.seat.reserve.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应 violation 表 */
@Data
public class Violation {

    /** 未签到 */
    public static final String NO_SHOW = "NO_SHOW";
    /** 暂离超时未归 */
    public static final String OVERTIME = "OVERTIME";

    private Long id;
    private Long userId;
    private Long bookingId;
    private String type;
    private LocalDateTime createdAt;
}