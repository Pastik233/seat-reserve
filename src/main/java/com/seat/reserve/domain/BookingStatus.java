package com.seat.reserve.domain;

/** 预约状态常量，避免到处写裸字符串 */
public final class BookingStatus {

    /** 已预约，待签到 */
    public static final String BOOKED = "BOOKED";
    /** 已签到，在座 */
    public static final String CHECKED_IN = "CHECKED_IN";
    /** 暂离中 */
    public static final String LEAVING = "LEAVING";
    /** 已完成 */
    public static final String FINISHED = "FINISHED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";
    /** 已失效（未签到 / 暂离超时） */
    public static final String EXPIRED = "EXPIRED";

    private BookingStatus() {
    }
}