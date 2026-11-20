package com.seat.reserve.mapper;

import com.seat.reserve.domain.SeatBooking;
import com.seat.reserve.web.dto.AdminBookingView;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingMapper {

    /**
     * 查该座位在该时段的第一条冲突预约，null 表示不冲突。
     * 重叠判据：已有开始 < 我的结束 AND 已有结束 > 我的开始（严格不等，半开区间）
     */
    SeatBooking findFirstConflict(@Param("seatId") Long seatId,
                                  @Param("bookDate") LocalDate bookDate,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    /** 某用户某天的有效预约条数（第 6 步限制每日 3 单用） */
    int countActiveByUserAndDate(@Param("userId") Long userId,
                                 @Param("bookDate") LocalDate bookDate);

    /** 某用户某天的有效预约（第 7 步"我的预约"用） */
    List<SeatBooking> listActiveByUserAndDate(@Param("userId") Long userId,
                                              @Param("bookDate") LocalDate bookDate);

    /** 按 id 查（取消时要用它的 owner 和当前状态） */
    SeatBooking findById(@Param("id") Long id);

    /** 插入，回填自增主键；version 由数据库默认值 1 起步 */
    int insert(SeatBooking booking);

    /**
     * 取消：WHERE 里带 status = 'BOOKED' 就是一次 CAS。
     * 返回 0 表示"状态已经变了"（被人抢着取消了/已签到），交给上层报 409。
     */
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    /** 该用户当天是否与此时段有重叠（跨座位也不能撞车） */
    int countUserOverlap(@Param("userId") Long userId,
                         @Param("bookDate") LocalDate bookDate,
                         @Param("startTime") LocalTime startTime,
                         @Param("endTime") LocalTime endTime);

    /** 签到：BOOKED → CHECKED_IN（返回 0 表示状态已变，下同） */
    int markCheckin(@Param("id") Long id);

    /** 暂离：CHECKED_IN → LEAVING */
    int markLeave(@Param("id") Long id);

    /** 回来：LEAVING → CHECKED_IN */
    int markBack(@Param("id") Long id);

    /** 系统：BOOKED → EXPIRED（未签到） */
    int markExpiredFromBooked(@Param("id") Long id);

    /** 系统：LEAVING → EXPIRED（暂离超时） */
    int markExpiredFromLeaving(@Param("id") Long id);

    /** 系统：结束 */
    int markFinished(@Param("id") Long id);

    List<AdminBookingView> listAllByDate(@Param("date") LocalDate date,
                                         @Param("status") String status);
}
