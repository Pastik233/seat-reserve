package com.seat.reserve.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis ZSet 的延迟队列。
 * score = 到期时刻的毫秒时间戳，member = 预约 id。
 * 只做"到期扫描"，不推送：@Scheduled 每 30 秒来看一眼有没有到期的。
 */
@Component
public class DelayQueue {

    /** 待签到：score = 预约开始时间 + 15 分钟 */
    public static final String CHECKIN = "delay:checkin";
    /** 待回来：score = 暂离时间 + 30 分钟 */
    public static final String LEAVE = "delay:leave";
    /** 待结束：score = 预约结束时间 */
    public static final String FINISH = "delay:finish";

    private final StringRedisTemplate redis;

    public DelayQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String key, Long bookingId, Instant dueAt) {
        redis.opsForZSet().add(key, String.valueOf(bookingId), dueAt.toEpochMilli());
    }

    public void remove(String key, Long bookingId) {
        redis.opsForZSet().remove(key, String.valueOf(bookingId));
    }

    /** 取出 score <= now 的成员，最多 limit 个；**只读不删**，处理完再 remove */
    public List<Long> pollDue(String key, int limit) {
        Set<String> members = redis.opsForZSet()
                .rangeByScore(key, 0, System.currentTimeMillis(), 0, limit);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream().map(Long::valueOf).toList();
    }

    public long size(String key) {
        Long n = redis.opsForZSet().zCard(key);
        return n == null ? 0 : n;
    }
}