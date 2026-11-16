package com.seat.reserve.mapper;

import org.apache.ibatis.annotations.Param;

public interface BookingLogMapper {

    int insert(@Param("bookingId") Long bookingId,
               @Param("fromStatus") String fromStatus,
               @Param("toStatus") String toStatus,
               @Param("operatorId") Long operatorId);
}