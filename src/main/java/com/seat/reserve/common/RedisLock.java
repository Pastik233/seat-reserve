package com.seat.reserve.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** 最简分布式锁：SET NX PX 加锁 + Lua 原子释放 */
@Component
public class RedisLock {

    /**
     * 释放锁必须是「比较再删除」：先 get 判断是不是自己的令牌，再 del。
     * Redis 单线程执行 Lua，所以这两步是原子的；直接 del 会删掉别人后来抢到的锁。
     */
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 抢到返回持有令牌（解锁要用），没抢到返回 null —— 不排队等待，直接让调用方返回"手慢了" */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    public void unlock(String key, String token) {
        redis.execute(UNLOCK, List.of(key), token);
    }
}