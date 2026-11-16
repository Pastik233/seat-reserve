package com.seat.reserve.mapper;

import com.seat.reserve.domain.LibRoom;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomMapper {

    /** 所有开放中的阅览室 */
    List<LibRoom> findAllOpen();

    LibRoom findById(@Param("id") Long id);
}