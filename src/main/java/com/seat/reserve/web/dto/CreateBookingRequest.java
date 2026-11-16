package com.seat.reserve.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** 创建预约请求体；时间按 ISO 格式传："09:00:00" */
public record CreateBookingRequest(
        @NotNull(message = "座位号不能为空") Long seatId,
        @NotNull(message = "日期不能为空") LocalDate bookDate,
        @NotNull(message = "开始时间不能为空") LocalTime startTime,
        @NotNull(message = "结束时间不能为空") LocalTime endTime,
        String remark) {
}