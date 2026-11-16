package com.seat.reserve.mapper;

import com.seat.reserve.domain.SysUser;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface UserMapper {

    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    int increaseViolationCount(@Param("userId") Long userId);

    Integer findViolationCount(@Param("userId") Long userId);

    /** 拉黑到指定时间，同时把计数清零，避免每违约一次都重写一次拉黑时间 */
    int blacklist(@Param("userId") Long userId, @Param("until") LocalDateTime until);
}