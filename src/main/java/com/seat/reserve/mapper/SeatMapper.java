package com.seat.reserve.mapper;

import com.seat.reserve.domain.LibSeat;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SeatMapper {

    /** 座位总数，第 2 步用来验证 MyBatis 是否打通 */
    long countAll();

    /** 某房间的全部座位（含维修中的，前端自己置灰） */
    List<LibSeat> listByRoom(@Param("roomId") Long roomId);

    /** 某房间可用座位数（status = 1，不含维修中的），用于「空闲 x / y」的 y */
    int countAvailableByRoom(@Param("roomId") Long roomId);

    LibSeat findById(@Param("id") Long id);

    /** 某房间在指定时段内空闲的座位（NOT EXISTS + 半开区间） */
    List<LibSeat> listFreeByRoom(@Param("roomId") Long roomId,
                                 @Param("bookDate") LocalDate bookDate,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime);

    /**
     * 第 2 层：行锁。SELECT ... FOR UPDATE 必须跑在事务里，
     * 事务提交前，别的请求执行到这里会一直阻塞 —— 这就是"串行化"的物理保证。
     */
    Long lockForUpdate(@Param("id") Long id);
}