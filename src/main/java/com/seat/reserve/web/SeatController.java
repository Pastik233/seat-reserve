package com.seat.reserve.web;

import com.seat.reserve.common.BizException;
import com.seat.reserve.common.R;
import com.seat.reserve.domain.SeatBooking;
import com.seat.reserve.service.SeatQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatQueryService seatQueryService;

    public SeatController(SeatQueryService seatQueryService) {
        this.seatQueryService = seatQueryService;
    }

    /** 只读冲突检测：冲突返回 409，空闲返回 200 */
    @GetMapping("/{seatId}/check")
    public R<Map<String, Object>> check(
            @PathVariable Long seatId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {

        SeatBooking conflict = seatQueryService.checkConflict(seatId, date, start, end);
        if (conflict != null) {
            throw BizException.conflict("该座位 " + conflict.getStartTime() + " - "
                    + conflict.getEndTime() + " 已被预约");
        }
        return R.ok(Map.of("seatId", seatId, "available", true));
    }
}