package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.LibRoom;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.domain.SeatBooking;
import com.seat.reserve.mapper.BookingMapper;
import com.seat.reserve.mapper.RoomMapper;
import com.seat.reserve.mapper.SeatMapper;
import com.seat.reserve.web.dto.FreeSeatsResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SeatQueryService {

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final BookingValidator validator;

    public SeatQueryService(RoomMapper roomMapper,
                            SeatMapper seatMapper,
                            BookingMapper bookingMapper,
                            BookingValidator validator) {
        this.roomMapper = roomMapper;
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.validator = validator;
    }

    /** 某房间指定时段的空闲座位。动态数据不走缓存，保证读到的就是真实占用。 */
    public FreeSeatsResult listFreeSeats(Long roomId, LocalDate date, LocalTime start, LocalTime end) {
        LibRoom room = roomMapper.findById(roomId);
        if (room == null) {
            throw BizException.notFound("阅览室不存在");
        }
        validator.validateRange(date, start, end, room);

        List<LibSeat> free = seatMapper.listFreeByRoom(roomId, date, start, end);
        // 分母必须是「可用座位数」：把维修中的算进来会让「空闲 x / y」对不上账，
        // 用户拿 y - x 当作已占用数，会多出几个根本不存在的占用。
        int totalSeats = seatMapper.countAvailableByRoom(roomId);
        return new FreeSeatsResult(totalSeats, free.size(), free);
    }

    /** 单座位冲突检测：不冲突返回 null，冲突返回那条预约 */
    public SeatBooking checkConflict(Long seatId, LocalDate date, LocalTime start, LocalTime end) {
        LibSeat seat = seatMapper.findById(seatId);
        if (seat == null) {
            throw BizException.notFound("座位不存在");
        }
        LibRoom room = roomMapper.findById(seat.getRoomId());
        validator.validateRange(date, start, end, room);
        return bookingMapper.findFirstConflict(seatId, date, start, end);
    }
}