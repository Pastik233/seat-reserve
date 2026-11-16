package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.LibRoom;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 预约时间规则：查空闲和创建预约共用一套校验 */
@Component
public class BookingValidator {

    private final Clock clock;

    /** Spring 走这个构造器：跟随系统默认时区 */
    public BookingValidator() {
        this(Clock.systemDefaultZone());
    }

    /** 测试用：注入固定时钟，让「今天 / 现在」这类判断可以复现 */
    BookingValidator(Clock clock) {
        this.clock = clock;
    }

    /** 预约粒度：30 分钟 */
    public static final int SLOT_MINUTES = 30;
    /** 单次最长：4 小时 */
    public static final int MAX_MINUTES = 240;
    /** 每日最多预约单数 */
    public static final int MAX_DAILY = 3;

    /**
     * 签到宽限：开始后 15 分钟内仍可签到（与 BookingStateService 的判定保持一致）。
     * 超过这个点再创建预约已经没有意义 —— 建出来立刻会被延迟队列判成 EXPIRED 并计一次违约。
     */
    public static final int CHECKIN_GRACE_MINUTES = 15;

    public void validateRange(LocalDate date, LocalTime start, LocalTime end, LibRoom room) {
        if (date.isBefore(LocalDate.now(clock))) {
            throw BizException.badRequest("不能查询或预约过去的日期");
        }
        if (!start.isBefore(end)) {
            throw BizException.badRequest("开始时间必须早于结束时间");
        }
        boolean slotAligned = start.getSecond() == 0 && end.getSecond() == 0
                && start.getMinute() % SLOT_MINUTES == 0
                && end.getMinute() % SLOT_MINUTES == 0;
        if (!slotAligned) {
            throw BizException.badRequest("时间必须是 " + SLOT_MINUTES + " 分钟的整数倍");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes > MAX_MINUTES) {
            throw BizException.badRequest("单次预约不能超过 " + (MAX_MINUTES / 60) + " 小时");
        }
        if (room.getOpenTime() != null && room.getCloseTime() != null) {
            if (start.isBefore(room.getOpenTime()) || end.isAfter(room.getCloseTime())) {
                throw BizException.badRequest("预约时段需在开放时间 "
                        + room.getOpenTime() + " - " + room.getCloseTime() + " 内");
            }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (date.equals(now.toLocalDate()) && !end.isAfter(now.toLocalTime())) {
            throw BizException.badRequest("结束时间已过，请选择更晚的时段");
        }

        // 开始时间 + 签到宽限都已经过去，说明这个预约创建出来也签不了到：
        // 延迟队列会在下一轮（≤30 秒）把它标成 EXPIRED，并给用户记一次 NO_SHOW 违约。
        // 用户什么都没干就吃一个违约，所以必须在入口就挡掉。
        if (LocalDateTime.of(date, start).plusMinutes(CHECKIN_GRACE_MINUTES).isBefore(now)) {
            throw BizException.badRequest("该时段已开始超过 " + CHECKIN_GRACE_MINUTES
                    + " 分钟，无法签到，请选择更晚的时段");
        }
    }
}