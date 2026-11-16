package com.seat.reserve.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应 booking_log 表：预约状态流转流水 */
@Data
public class BookingLog {

    private Long id;
    private Long bookingId;
    /** 原状态，创建时为空串 */
    private String fromStatus;
    private String toStatus;
    /** 0 = 系统自动 */
    private Long operatorId;
    private LocalDateTime createdAt;
}