package com.seat.reserve.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** 用户权限快照的 Redis 读写：auth:user:{userId} → JSON */
@Component
public class UserSnapshotStore {

    private static final String KEY_PREFIX = "auth:user:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public UserSnapshotStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(UserSnapshot snapshot, Duration ttl) {
        try {
            redis.opsForValue().set(key(snapshot.id()), objectMapper.writeValueAsString(snapshot), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化用户快照失败", e);
        }
    }

    public Optional<UserSnapshot> get(Long userId) {
        String json = redis.opsForValue().get(key(userId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, UserSnapshot.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** 踢下线 / 封号时调这个，所有已签发的 token 立刻失效 */
    public void evict(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}