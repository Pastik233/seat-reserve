package com.seat.reserve.web.dto;

import java.math.BigDecimal;

/** 排行通用行：seatUsage 与 violationRank 都用它 */
public record RankRow(
    Long id,            // seatId 或 userId
    String name,        // 座位号 或 用户昵称
    String extra,       // 阅览室名 或 用户名
    long times,         // 次数
    long noShow,        // NO_SHOW 次数（座位排行恒为 0）
    BigDecimal hours    // 使用小时数（违约排行恒为 0）
) {
}
