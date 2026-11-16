package com.seat.reserve.service;

import com.seat.reserve.common.AfterCommit;
import com.seat.reserve.common.BizException;
import com.seat.reserve.common.RedisLock;
import com.seat.reserve.domain.*;
import com.seat.reserve.mapper.*;
import com.seat.reserve.security.UserSnapshot;
import com.seat.reserve.web.dto.BookingView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Slf4j
@Service
public class BookingService {

    /** 锁租期：临界区只有几条 SQL，10 秒绰绰有余；宁可等超时重试，也别设太长拖死座位 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final RedisLock redisLock;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;
    private final BookingLogMapper bookingLogMapper;
    private final BookingValidator validator;
    private final DelayQueue delayQueue;
    private final UserMapper userMapper;

    public BookingService(RedisLock redisLock, SeatMapper seatMapper, RoomMapper roomMapper,
                          BookingMapper bookingMapper, BookingLogMapper bookingLogMapper,
                          BookingValidator validator, DelayQueue delayQueue, UserMapper userMapper) {
        this.redisLock = redisLock;
        this.seatMapper = seatMapper;
        this.roomMapper = roomMapper;
        this.bookingMapper = bookingMapper;
        this.bookingLogMapper = bookingLogMapper;
        this.validator = validator;
        this.delayQueue = delayQueue;
        this.userMapper = userMapper;
    }

    /**
     * 三层防重：① Redis 分布式锁挡流量 ② 数据库行锁保证串行 ③ 半开区间冲突检测保证语义
     * 方法上必须有 @Transactional：FOR UPDATE 的行锁生命周期 = 事务生命周期
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingView create(UserSnapshot me, Long seatId, LocalDate date,
                              LocalTime start, LocalTime end, String remark) {
        // —— 锁外的前置校验：快速失败，不浪费抢锁和数据库资源 ——
        LibSeat seat = seatMapper.findById(seatId);
        if (seat == null) {
            throw BizException.notFound("座位不存在");
        }
        if (seat.getStatus() != null && seat.getStatus() == 0) {
            throw BizException.badRequest("该座位维修中，暂不可预约");
        }
        LibRoom room = roomMapper.findById(seat.getRoomId());
        if (room == null) {
            throw BizException.notFound("阅览室不存在");
        }
        validator.validateRange(date, start, end, room);

        SysUser dbUser = userMapper.findById(me.id());
        if (dbUser != null && dbUser.getBlacklistUntil() != null
                && dbUser.getBlacklistUntil().isAfter(LocalDateTime.now())) {
            throw BizException.forbidden("你因累计违约已被限制预约，解禁时间 " + dbUser.getBlacklistUntil());
        }

        if (bookingMapper.countActiveByUserAndDate(me.id(), date) >= BookingValidator.MAX_DAILY) {
            throw BizException.conflict("每天最多预约 " + BookingValidator.MAX_DAILY + " 个时段");
        }

        // —— 第 1 层：Redis 分布式锁，粒度 = 座位 + 日期 ——
        String lockKey = "lock:seat:" + seatId + ":" + date;
        String token = redisLock.tryLock(lockKey, LOCK_TTL);
        if (token == null) {
            throw BizException.conflict("该座位正被他人预约，请稍后重试");
        }
        try {
            // —— 第 2 层：数据库行锁（并发请求在这里排队，一个个进来）——
            Long locked = seatMapper.lockForUpdate(seatId);
            if (locked == null) {
                throw BizException.notFound("座位不存在");
            }
            // —— 第 3 层：拿到锁之后再查冲突，这次的结果才是可信的 ——
            SeatBooking conflict = bookingMapper.findFirstConflict(seatId, date, start, end);
            if (conflict != null) {
                throw BizException.conflict("该座位 " + conflict.getStartTime()
                        + " - " + conflict.getEndTime() + " 已被预约");
            }
            if (bookingMapper.countUserOverlap(me.id(), date, start, end) > 0) {
                throw BizException.conflict("你在这个时段已有其他预约，避免撞车");
            }
            // 锁内再查一次每日额度，锁外的检查只是乐观预判
            if (bookingMapper.countActiveByUserAndDate(me.id(), date) >= BookingValidator.MAX_DAILY) {
                throw BizException.conflict("每天最多预约 " + BookingValidator.MAX_DAILY + " 个时段");
            }

            SeatBooking booking = new SeatBooking();
            booking.setSeatId(seatId);
            booking.setRoomId(seat.getRoomId());
            booking.setUserId(me.id());
            booking.setBookDate(date);
            booking.setStartTime(start);
            booking.setEndTime(end);
            booking.setStatus(BookingStatus.BOOKED);
            booking.setRemark(remark == null ? "" : remark);
            bookingMapper.insert(booking);

            LocalDateTime startAt = LocalDateTime.of(date, start);
            LocalDateTime endAt = LocalDateTime.of(date, end);
            ZoneId zone = ZoneId.systemDefault();
            Long bookingId = booking.getId();
            // 延迟任务必须等事务提交后再入队：否则事务回滚会在 Redis 里留下指向
            // 不存在预约的幽灵任务；万一 score 已过期，下一轮扫描就会把它捞出来空转。
            AfterCommit.run(() -> {
                // 开始后 15 分钟还没签到 → 作废
                delayQueue.add(DelayQueue.CHECKIN, bookingId,
                        startAt.plusMinutes(BookingStateService.CHECKIN_LATE_MINUTES)
                                .atZone(zone).toInstant());
                // 到结束时间 → FINISHED
                delayQueue.add(DelayQueue.FINISH, bookingId, endAt.atZone(zone).toInstant());
            });
            // 库里的 created_at 是 DEFAULT CURRENT_TIMESTAMP，MyBatis 拿不回来，这里补一个给前端
            booking.setCreatedAt(LocalDateTime.now());

            // 任何状态变化都留痕，第 7 步的监管/统计全靠这张表
            bookingLogMapper.insert(booking.getId(), "", BookingStatus.BOOKED, me.id());

            log.info("用户 {} 预约座位 {} {} {}~{} 成功，预约号 {}",
                    me.username(), seat.getSeatNo(), date, start, end, booking.getId());
            return BookingView.of(booking, seat, room);
        } finally {
            // 事务还没提交就放锁也没问题：此后的请求会卡在 FOR UPDATE 上，直到本事务提交
            redisLock.unlock(lockKey, token);
        }
    }

    /** 取消：只有 BOOKED 能取消，且只能取消自己的 */
    @Transactional
    public void cancel(UserSnapshot me, Long bookingId) {
        SeatBooking booking = bookingMapper.findById(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        boolean isOwner = booking.getUserId().equals(me.id());
        boolean isAdmin = "ADMIN".equals(me.roleCode());
        if (!isOwner && !isAdmin) {
            throw BizException.forbidden("不能取消他人的预约");
        }
        int rows = bookingMapper.cancel(bookingId, booking.getUserId());
        if (rows == 0) {
            // 并发的另一方先取消了，或者已经签到/过期
            throw BizException.conflict("当前状态不可取消，只有「待使用」的预约可以取消");
        }
        bookingLogMapper.insert(bookingId, booking.getStatus(),
                BookingStatus.CANCELLED, me.id());
        // 事务回滚时这两个任务不能摘，否则预约还是 BOOKED、却永远不会自动过期，座位被占死
        AfterCommit.run(() -> {
            delayQueue.remove(DelayQueue.CHECKIN, bookingId);
            delayQueue.remove(DelayQueue.FINISH, bookingId);
        });
        log.info("预约 {} 被 {} 取消", bookingId, me.username());
    }
}