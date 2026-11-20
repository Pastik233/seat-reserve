package com.seat.reserve.web;

import com.seat.reserve.common.R;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.security.UserSnapshot;
import com.seat.reserve.service.BookingService;
import com.seat.reserve.service.ReportService;
import com.seat.reserve.service.SeatAdminService;
import com.seat.reserve.web.dto.AdminBookingView;
import com.seat.reserve.web.dto.OverviewReport;
import com.seat.reserve.web.dto.RankRow;
import com.seat.reserve.web.dto.SeatUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")   // SecurityConfig 已 @EnableMethodSecurity，直接生效
public class AdminController {

    private final ReportService reportService;
    private final SeatAdminService seatAdminService;
    private final BookingService bookingService;

    public AdminController(ReportService reportService, SeatAdminService seatAdminService, BookingService bookingService) {
        this.reportService = reportService;
        this.seatAdminService = seatAdminService;
        this.bookingService = bookingService;
    }

    @GetMapping("/overview")
    public R<OverviewReport> overview(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.overview(from, to));
    }

    @GetMapping("/seats/rank")
    public R<List<RankRow>> seatRank(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.seatUsage(from, to));
    }

    @GetMapping("/violations/rank")
    public R<List<RankRow>> violationRank(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.violationRank(from, to));
    }

    @GetMapping("/heat")
    public R<List<RankRow>> heat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.hourlyHeat(from, to));
    }

    // ==================== 座位管理 ====================

    /** 某阅览室的全部座位（含维修中），管理端列表直读数据库 */
    @GetMapping("/seats")
    public R<List<LibSeat>> seats(@RequestParam Long roomId) {
        return R.ok(seatAdminService.listByRoom(roomId));
    }

    /** 新增座位 */
    @PostMapping("/seats")
    public R<LibSeat> createSeat(@Valid @RequestBody SeatUpsertRequest req) {
        return R.ok(seatAdminService.create(req));
    }

    /** 修改座位（含置维修 / 恢复），roomId 与 seatNo 不可变 */
    @PutMapping("/seats/{id}")
    public R<LibSeat> updateSeat(@PathVariable Long id,
                                 @Valid @RequestBody SeatUpsertRequest req) {
        return R.ok(seatAdminService.update(id, req));
    }

    /** 删除座位（仅允许删除无预约记录的座位） */
    @DeleteMapping("/seats/{id}")
    public R<Void> deleteSeat(@PathVariable Long id) {
        seatAdminService.delete(id);
        return R.ok(null);
    }

    // ==================== 预约管理 ====================

    /** 管理端：按日期查全部预约，status 可选（不传则查全部） */
    @GetMapping("/bookings")
    public R<List<AdminBookingView>> bookings(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String status) {
        // STATUS_META 之外的裸字符串直接当无条件处理会更宽松，这里选择严格：只接受已知状态
        if (status != null && !status.isBlank()
            && !java.util.List.of("BOOKED", "CHECKED_IN", "LEAVING",
            "FINISHED", "CANCELLED", "EXPIRED").contains(status)) {
            throw com.seat.reserve.common.BizException.badRequest("未知的状态值");
        }
        return R.ok(reportService.listAllBookings(date, status));
    }

    /** 管理端：强制取消预约（复用用户自助取消的同一套逻辑） */
    @DeleteMapping("/bookings/{id}")
    public R<Void> cancelBooking(@AuthenticationPrincipal UserSnapshot me,
                                 @PathVariable Long id) {
        bookingService.cancel(me, id);   // 复用！cancel 内部已判断 ADMIN 权限并记操作人
        return R.ok(null);
    }
}
