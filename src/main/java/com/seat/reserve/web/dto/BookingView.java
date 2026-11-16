package com.seat.reserve.web.dto;

import com.seat.reserve.domain.LibRoom;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.domain.SeatBooking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 返回给前端的预约详情：带上座位号和阅览室名，前端不用再查一次 */
public record BookingView(
        Long id,
        Long seatId,
        String seatNo,
        Long roomId,
        String roomName,
        LocalDate bookDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String remark,
        LocalDateTime createdAt) {

    public static BookingView of(SeatBooking b, LibSeat seat, LibRoom room) {
        return new BookingView(b.getId(), b.getSeatId(),
                seat == null ? null : seat.getSeatNo(), b.getRoomId(),
                room == null ? null : room.getName(), b.getBookDate(),
                b.getStartTime(), b.getEndTime(), b.getStatus(),
                b.getRemark(), b.getCreatedAt());
    }
}