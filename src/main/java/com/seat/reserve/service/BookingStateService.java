package com.seat.reserve.service;

import com.seat.reserve.common.AfterCommit;
import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.BookingStatus;
import com.seat.reserve.domain.SeatBooking;
import com.seat.reserve.domain.Violation;
import com.seat.reserve.mapper.BookingLogMapper;
import com.seat.reserve.mapper.BookingMapper;
import com.seat.reserve.security.UserSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 预约状态机：所有状态变化都必须走 CAS 更新（UPDATE ... WHERE status = '期望值'）。
 * 返回受影响行数，0 表示状态已经被别人改过了 —— 这就是并发下的"只能改一次"。
 */
@Slf4j
@Service
public class BookingStateService {

    /** 签到窗口：开始前 15 分钟 */
    public static final int CHECKIN_EARLY_MINUTES = 15;
    /** 签到窗口：开始后 15 分钟（与 BookingValidator 共用一个常量，避免两处漂移） */
    public static final int CHECKIN_LATE_MINUTES = BookingValidator.CHECKIN_GRACE_MINUTES;
    /** 暂离允许时长（分钟） */
    public static final int LEAVE_MINUTES = 30;

    private final BookingMapper bookingMapper;
    private final BookingLogMapper bookingLogMapper;
    private final DelayQueue delayQueue;
    private final ViolationService violationService;
    /**
     * 批量回收用「每条一个事务」：状态变更、写日志、记违约必须同生共死，
     * 但一条失败又不能带着整批回滚，所以不能简单地在方法上加 @Transactional。
     */
    private final TransactionTemplate tx;

    public BookingStateService(BookingMapper bookingMapper, BookingLogMapper bookingLogMapper,
                               DelayQueue delayQueue, ViolationService violationService,
                               PlatformTransactionManager txManager) {
        this.bookingMapper = bookingMapper;
        this.bookingLogMapper = bookingLogMapper;
        this.delayQueue = delayQueue;
        this.violationService = violationService;
        this.tx = new TransactionTemplate(txManager);
    }

    // ==================== 用户操作 ====================

    /** 签到：BOOKED → CHECKED_IN，必须落在「开始前 15 分钟 ~ 开始后 15 分钟」窗口内 */
    @Transactional
    public SeatBooking checkin(UserSnapshot me, Long bookingId) {
        SeatBooking booking = mustBeMine(me, bookingId);
        LocalDateTime start = LocalDateTime.of(booking.getBookDate(), booking.getStartTime());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(start.minusMinutes(CHECKIN_EARLY_MINUTES))) {
            throw BizException.badRequest("还没到签到时间，最早 "
                    + start.minusMinutes(CHECKIN_EARLY_MINUTES).toLocalTime());
        }
        if (now.isAfter(start.plusMinutes(CHECKIN_LATE_MINUTES))) {
            throw BizException.badRequest("已超过签到时间，该预约已作废");
        }
        if (bookingMapper.markCheckin(bookingId) == 0) {
            throw BizException.conflict("当前状态不可签到（可能已取消或已失效）");
        }
        bookingLogMapper.insert(bookingId, BookingStatus.BOOKED, BookingStatus.CHECKED_IN, me.id());
        // 签到了，把"催签到"的任务摘掉，别让它在 30 秒后把人判成 NO_SHOW。
        // 事务回滚时这条就不能摘，所以推到提交之后。
        AfterCommit.run(() -> delayQueue.remove(DelayQueue.CHECKIN, bookingId));
        return bookingMapper.findById(bookingId);
    }

    /** 暂离：CHECKED_IN → LEAVING，入队「30 分钟后必须回来」 */
    @Transactional
    public SeatBooking leave(UserSnapshot me, Long bookingId) {
        mustBeMine(me, bookingId);
        if (bookingMapper.markLeave(bookingId) == 0) {
            throw BizException.conflict("只有「已在座」的预约可以暂离");
        }
        bookingLogMapper.insert(bookingId, BookingStatus.CHECKED_IN, BookingStatus.LEAVING, me.id());
        // 到期时刻在事务内算好，避免 afterCommit 执行时又晚了若干毫秒
        Instant dueAt = LocalDateTime.now().plusMinutes(LEAVE_MINUTES)
                .atZone(ZoneId.systemDefault()).toInstant();
        AfterCommit.run(() -> delayQueue.add(DelayQueue.LEAVE, bookingId, dueAt));
        return bookingMapper.findById(bookingId);
    }

    /** 回来：LEAVING → CHECKED_IN，撤掉超时任务 */
    @Transactional
    public SeatBooking back(UserSnapshot me, Long bookingId) {
        mustBeMine(me, bookingId);
        if (bookingMapper.markBack(bookingId) == 0) {
            throw BizException.conflict("只有「暂离中」的预约可以回来");
        }
        bookingLogMapper.insert(bookingId, BookingStatus.LEAVING, BookingStatus.CHECKED_IN, me.id());
        AfterCommit.run(() -> delayQueue.remove(DelayQueue.LEAVE, bookingId));
        return bookingMapper.findById(bookingId);
    }

    // ==================== 系统回收（延迟队列驱动） ====================

    /** 到期还没签到 → EXPIRED + 记 NO_SHOW 违约 */
    public void handleCheckinTimeout(List<Long> bookingIds) {
        for (Long id : bookingIds) {
            try {
                // 一条一个事务：CAS 成功但记违约失败时必须整体回滚，
                // 否则状态已经改了，下一轮 CAS 返回 0，这次违约就永久丢了。
                if (Boolean.TRUE.equals(tx.execute(status -> expireNoShow(id)))) {
                    log.warn("预约 {} 超时未签到，已作废并计违约", id);
                }
                // CAS 失败说明状态早就变了（签过了/取消了），任务一样要摘掉，否则每轮都被扫出来空转
                delayQueue.remove(DelayQueue.CHECKIN, id);
            } catch (Exception e) {
                // 事务已回滚、任务保留，30 秒后自动重试；继续处理同批其它任务
                log.error("处理待签到超时任务 {} 失败，保留待重试", id, e);
            }
        }
    }

    /** 暂离超时 → EXPIRED + 记 OVERTIME 违约 */
    public void handleLeaveTimeout(List<Long> bookingIds) {
        for (Long id : bookingIds) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> expireOvertime(id)))) {
                    log.warn("预约 {} 暂离超时，已作废并计违约", id);
                }
                delayQueue.remove(DelayQueue.LEAVE, id);
            } catch (Exception e) {
                log.error("处理暂离超时任务 {} 失败，保留待重试", id, e);
            }
        }
    }

    /** 到结束时间 → FINISHED（在座/暂离的才算，未签到的早就被上面的任务作废了） */
    public void handleFinish(List<Long> bookingIds) {
        for (Long id : bookingIds) {
            try {
                tx.execute(status -> finish(id));
                delayQueue.remove(DelayQueue.FINISH, id);
            } catch (Exception e) {
                log.error("处理结束任务 {} 失败，保留待重试", id, e);
            }
        }
    }

    // ==================== 单条事务体 ====================

    private boolean expireNoShow(Long id) {
        SeatBooking booking = bookingMapper.findById(id);
        if (booking == null || bookingMapper.markExpiredFromBooked(id) == 0) {
            return false;
        }
        bookingLogMapper.insert(id, booking.getStatus(), BookingStatus.EXPIRED, 0L);
        violationService.record(booking.getUserId(), id, Violation.NO_SHOW);
        return true;
    }

    private boolean expireOvertime(Long id) {
        SeatBooking booking = bookingMapper.findById(id);
        if (booking == null || bookingMapper.markExpiredFromLeaving(id) == 0) {
            return false;
        }
        bookingLogMapper.insert(id, booking.getStatus(), BookingStatus.EXPIRED, 0L);
        violationService.record(booking.getUserId(), id, Violation.OVERTIME);
        return true;
    }

    private boolean finish(Long id) {
        SeatBooking booking = bookingMapper.findById(id);
        if (booking == null || bookingMapper.markFinished(id) == 0) {
            return false;
        }
        // from_status 取真实旧状态：markFinished 的 WHERE 允许 CHECKED_IN 和 LEAVING 两种
        bookingLogMapper.insert(id, booking.getStatus(), BookingStatus.FINISHED, 0L);
        return true;
    }

    private SeatBooking mustBeMine(UserSnapshot me, Long bookingId) {
        SeatBooking booking = bookingMapper.findById(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!booking.getUserId().equals(me.id())) {
            throw BizException.forbidden("不能操作他人的预约");
        }
        return booking;
    }
}
