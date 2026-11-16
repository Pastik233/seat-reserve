package com.seat.reserve.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** 对应 lib_room 表 */
@Data
public class LibRoom {

    private Long id;
    private String name;
    private String floor;
    private LocalTime openTime;
    private LocalTime closeTime;
    /** 1 开放 0 关闭 */
    private Integer status;
    private LocalDateTime createdAt;
}