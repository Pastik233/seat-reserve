package com.seat.reserve.domain;

import lombok.Data;

/** 对应 lib_seat 表 */
@Data
public class LibSeat {

    private Long id;
    private Long roomId;
    private String seatNo;
    private String zone;
    /** 1 有插座 0 无 */
    private Integer hasPower;
    /** 1 可用 0 维修 */
    private Integer status;
}