package com.seat.reserve.web.dto;

import java.math.BigDecimal;

/** 运营看板核心指标：一次查询全部拿到 */
public record OverviewReport(
    long total,          // 区间内预约总数
    long finished,       // 履约完成数
    long expired,        // 违约作废数
    long cancelled,      // 用户取消数
    BigDecimal fulfillRate,  // 履约率 = finished/(finished+expired)，去掉主动取消
    BigDecimal bookedHours   // 累计被预约小时数
) {
}
