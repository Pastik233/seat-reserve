package com.seat.reserve.web;

import com.seat.reserve.common.R;
import com.seat.reserve.domain.SeatBooking;
import com.seat.reserve.mapper.BookingMapper;
import com.seat.reserve.security.UserSnapshot;
import com.seat.reserve.service.BookingService;
import com.seat.reserve.service.BookingStateService;
import com.seat.reserve.web.dto.BookingView;
import com.seat.reserve.web.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final BookingStateService stateService;

    public BookingController(BookingService bookingService, BookingMapper bookingMapper, BookingStateService stateService) {
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
        this.stateService = stateService;
    }

    /** 创建预约 */
    @PostMapping
    public R<BookingView> create(@AuthenticationPrincipal UserSnapshot me,
                                 @Valid @RequestBody CreateBookingRequest req) {
        return R.ok(bookingService.create(me, req.seatId(), req.bookDate(),
                req.startTime(), req.endTime(), req.remark()));
    }

    /** 取消预约 */
    @DeleteMapping("/{id}")
    public R<Void> cancel(@AuthenticationPrincipal UserSnapshot me, @PathVariable Long id) {
        bookingService.cancel(me, id);
        return R.ok(null);
    }

    /** 我的预约（某天） */
    @GetMapping("/my")
    public R<List<SeatBooking>> my(@AuthenticationPrincipal UserSnapshot me,
                                   @RequestParam
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(bookingMapper.listActiveByUserAndDate(me.id(), date));
    }

    /** 签到 */
    @PostMapping("/{id}/checkin")
    public R<SeatBooking> checkin(@AuthenticationPrincipal UserSnapshot me, @PathVariable Long id) {
        return R.ok(stateService.checkin(me, id));
    }

    /** 暂离 */
    @PostMapping("/{id}/leave")
    public R<SeatBooking> leave(@AuthenticationPrincipal UserSnapshot me, @PathVariable Long id) {
        return R.ok(stateService.leave(me, id));
    }

    /** 回来 */
    @PostMapping("/{id}/back")
    public R<SeatBooking> back(@AuthenticationPrincipal UserSnapshot me, @PathVariable Long id) {
        return R.ok(stateService.back(me, id));
    }
}