package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.mapper.BookingMapper;
import com.seat.reserve.mapper.ReportMapper;
import com.seat.reserve.web.dto.AdminBookingView;
import com.seat.reserve.web.dto.OverviewReport;
import com.seat.reserve.web.dto.RankRow;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private static final int MAX_RANGE_DAYS = 92;   // 防止一次扫全表

    private final ReportMapper reportMapper;
    private final BookingMapper bookingMapper;

    public ReportService(ReportMapper reportMapper, BookingMapper bookingMapper) {
        this.reportMapper = reportMapper;
        this.bookingMapper = bookingMapper;
    }

    public OverviewReport overview(LocalDate from, LocalDate to) {
        checkRange(from, to);
        return reportMapper.overview(from, to);
    }

    public List<RankRow> seatUsage(LocalDate from, LocalDate to) {
        checkRange(from, to);
        return reportMapper.seatUsageRank(from, to, 20);
    }

    public List<RankRow> violationRank(LocalDate from, LocalDate to) {
        checkRange(from, to);
        return reportMapper.violationRank(from, to, 20);
    }

    public List<RankRow> hourlyHeat(LocalDate from, LocalDate to) {
        checkRange(from, to);
        return reportMapper.hourlyHeat(from, to);
    }

    private void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw BizException.badRequest("日期区间不合法");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw BizException.badRequest("单次最多查询 " + MAX_RANGE_DAYS + " 天");
        }
    }

    public List<AdminBookingView> listAllBookings(LocalDate date, String status) {
        return bookingMapper.listAllByDate(date, status);
    }
}
