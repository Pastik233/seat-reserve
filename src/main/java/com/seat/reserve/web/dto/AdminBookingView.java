package com.seat.reserve.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理端预约列表：比用户端 BookingView 多了 username / nickname，便于追溯是谁预约的。
 */
public record AdminBookingView(
    Long id,
    Long seatId,
    String seatNo,
    Long roomId,
    String roomName,
    Long userId,
    String username,
    String nickname,
    LocalDate bookDate,
    LocalTime startTime,
    LocalTime endTime,
    String status,
    LocalDateTime createdAt
) {
}
