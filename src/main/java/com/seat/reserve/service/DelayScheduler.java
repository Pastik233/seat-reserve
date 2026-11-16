package com.seat.reserve.service;

import com.seat.reserve.common.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/** 每 30 秒扫一次三个延迟队列；用 Redis 锁保证多实例部署时同一轮只有一个实例在跑 */
@Slf4j
@Component
public class DelayScheduler {

    /** 每轮扫描最多处理 200 条，剩下的下一轮来 */
    private static final int BATCH = 200;
    /** 锁租期要比重跑间隔短一点，实例被 kill 后锁会自动过期 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(25);

    private final DelayQueue delayQueue;
    private final BookingStateService stateService;
    private final RedisLock redisLock;

    public DelayScheduler(DelayQueue delayQueue, BookingStateService stateService, RedisLock redisLock) {
        this.delayQueue = delayQueue;
        this.stateService = stateService;
        this.redisLock = redisLock;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void sweep() {
        run(DelayQueue.CHECKIN, "lock:delay:checkin", stateService::handleCheckinTimeout);
        run(DelayQueue.LEAVE, "lock:delay:leave", stateService::handleLeaveTimeout);
        run(DelayQueue.FINISH, "lock:delay:finish", stateService::handleFinish);
    }

    private void run(String queueKey, String lockKey, Consumer<List<Long>> handler) {
        String token = redisLock.tryLock(lockKey, LOCK_TTL);
        if (token == null) {
            log.debug("{} 这一轮被其它实例接管，跳过", queueKey);
            return;
        }
        try {
            List<Long> due = delayQueue.pollDue(queueKey, BATCH);
            if (due.isEmpty()) {
                return;
            }
            log.info("{} 扫到 {} 条到期任务", queueKey, due.size());
            handler.accept(due);
        } catch (Exception e) {
            // 抛出去会让 @Scheduled 停掉后续轮次，这里必须自己吞掉
            log.error("扫描 {} 失败", queueKey, e);
        } finally {
            redisLock.unlock(lockKey, token);
        }
    }
}