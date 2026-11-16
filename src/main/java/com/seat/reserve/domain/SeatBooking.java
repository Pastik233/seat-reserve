package com.seat.reserve.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 对应 seat_booking 表 */
@Data
public class SeatBooking {

    private Long id;
    private Long seatId;
    private Long roomId;
    private Long userId;
    private LocalDate bookDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime checkinAt;
    /** 最近一次暂离时间 */
    private LocalDateTime leaveAt;
    /** 乐观锁版本号 */
    private Integer version;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}