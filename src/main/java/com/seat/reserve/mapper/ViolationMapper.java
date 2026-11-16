package com.seat.reserve.mapper;

import com.seat.reserve.domain.Violation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ViolationMapper {

    int insert(@Param("userId") Long userId,
               @Param("bookingId") Long bookingId,
               @Param("type") String type);

    List<Violation> listByUser(@Param("userId") Long userId);
}