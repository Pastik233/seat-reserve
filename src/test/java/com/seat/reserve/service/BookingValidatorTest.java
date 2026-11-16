package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.LibRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 预约时间规则单测。
 *
 * <p>用固定时钟把「现在」钉在 2026-11-16 10:07（北京时间），
 * 这样「当天已开始多久」这类判断不依赖跑测试的具体时刻，CI 上任何时间跑结果都一样。
 */
class BookingValidatorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 2026-11-16 02:07 UTC = 北京时间 10:07 */
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-11-16T02:07:00Z"), ZONE);

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private BookingValidator validator;
    /** 一楼自习室：08:00 - 22:00 */
    private LibRoom room;

    @BeforeEach
    void setUp() {
        validator = new BookingValidator(FIXED);
        room = room(LocalTime.of(8, 0), LocalTime.of(22, 0));
    }

    @Test
    @DisplayName("过去的日期直接拒绝")
    void rejectsPastDate() {
        assertRejected(TODAY.minusDays(1), 9, 0, 10, 0, "过去的日期");
    }

    @Test
    @DisplayName("开始时间不早于结束时间")
    void rejectsInvertedRange() {
        assertRejected(TOMORROW, 10, 0, 10, 0, "开始时间必须早于结束时间");
        assertRejected(TOMORROW, 11, 0, 10, 0, "开始时间必须早于结束时间");
    }

    @Test
    @DisplayName("时间必须是 30 分钟的整数倍")
    void rejectsUnalignedSlot() {
        assertRejected(TOMORROW, 10, 15, 11, 0, "30 分钟的整数倍");
        assertRejected(TOMORROW, 10, 0, 11, 45, "30 分钟的整数倍");
    }

    @Test
    @DisplayName("单次预约不能超过 4 小时")
    void rejectsTooLong() {
        assertRejected(TOMORROW, 8, 0, 12, 30, "不能超过 4 小时");
    }

    @Test
    @DisplayName("正好 4 小时应当通过")
    void allowsExactlyFourHours() {
        assertAllowed(TOMORROW, 8, 0, 12, 0);
    }

    @Test
    @DisplayName("时段必须落在阅览室开放时间内")
    void rejectsOutsideOpeningHours() {
        assertRejected(TOMORROW, 7, 30, 9, 0, "开放时间");
        assertRejected(TOMORROW, 21, 0, 22, 30, "开放时间");
    }

    @Test
    @DisplayName("不同阅览室按各自的开放时间校验")
    void usesEachRoomOpeningHours() {
        LibRoom readingRoom = room(LocalTime.of(8, 30), LocalTime.of(21, 30));
        assertThatCode(() -> validator.validateRange(TOMORROW, LocalTime.of(8, 30), LocalTime.of(9, 0), readingRoom))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateRange(TOMORROW, LocalTime.of(8, 0), LocalTime.of(9, 0), readingRoom))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("开放时间");
    }

    @Test
    @DisplayName("当天时段已经结束，不能再约")
    void rejectsEndedSlotToday() {
        assertRejected(TODAY, 9, 0, 10, 0, "结束时间已过");
    }

    @Test
    @DisplayName("当天已开始超过签到宽限，拒绝创建（否则建出来必然失效并倒扣违约）")
    void rejectsSlotStartedBeyondGrace() {
        // 09:30 开始，宽限 15 分钟到 09:45，此刻 10:07 早已错过
        assertRejected(TODAY, 9, 30, 10, 30, "已开始超过");
    }

    @Test
    @DisplayName("当天刚开始几分钟，仍在签到宽限内，允许预约")
    void allowsSlotWithinGrace() {
        // 10:00 开始，10:07 仍在 15 分钟宽限内
        assertAllowed(TODAY, 10, 0, 11, 0);
    }

    @Test
    @DisplayName("明天的正常时段通过")
    void allowsTomorrowSlot() {
        assertAllowed(TOMORROW, 8, 0, 10, 0);
        assertAllowed(TOMORROW, 21, 0, 22, 0);
    }

    private LibRoom room(LocalTime open, LocalTime close) {
        LibRoom r = new LibRoom();
        r.setName("测试阅览室");
        r.setOpenTime(open);
        r.setCloseTime(close);
        r.setStatus(1);
        return r;
    }

    private void assertRejected(LocalDate date, int startHour, int startMinute,
                                int endHour, int endMinute, String expectedMessage) {
        assertThatThrownBy(() -> validator.validateRange(
                date, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute), room))
                .as("%s %02d:%02d-%02d:%02d 应被拒绝", date, startHour, startMinute, endHour, endMinute)
                .isInstanceOf(BizException.class)
                .hasMessageContaining(expectedMessage);
    }

    private void assertAllowed(LocalDate date, int startHour, int startMinute, int endHour, int endMinute) {
        assertThatCode(() -> validator.validateRange(
                date, LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute), room))
                .as("%s %02d:%02d-%02d:%02d 应当通过", date, startHour, startMinute, endHour, endMinute)
                .doesNotThrowAnyException();
    }
}
