package com.seat.reserve.web;

import com.seat.reserve.common.R;
import com.seat.reserve.domain.LibRoom;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.service.RoomService;
import com.seat.reserve.service.SeatQueryService;
import com.seat.reserve.web.dto.FreeSeatsResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private SeatQueryService seatQueryService;

    public RoomController(RoomService roomService,SeatQueryService seatQueryService) {
        this.roomService = roomService;
        this.seatQueryService=seatQueryService;
    }

    /** 阅览室列表 */
    @GetMapping
    public R<List<LibRoom>> list() {
        return R.ok(roomService.listRooms());
    }

    /** 某阅览室的座位列表 */
    @GetMapping("/{roomId}/seats")
    public R<List<LibSeat>> seats(@PathVariable Long roomId) {
        return R.ok(roomService.listSeats(roomId));
    }

    /** 指定时段空闲座位 */
    @GetMapping("/{roomId}/seats/free")
    public R<FreeSeatsResult> freeSeats(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {
        return R.ok(seatQueryService.listFreeSeats(roomId, date, start, end));
    }
}