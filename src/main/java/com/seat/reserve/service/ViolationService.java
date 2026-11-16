package com.seat.reserve.service;

import com.seat.reserve.mapper.UserMapper;
import com.seat.reserve.mapper.ViolationMapper;
import com.seat.reserve.security.UserSnapshotStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 违约登记 + 拉黑。被延迟计划任务调用，operator 是系统 */
@Slf4j
@Service
public class ViolationService {

    /** 累计违约多少次拉黑 */
    public static final int MAX_BEFORE_BLACKLIST = 3;
    /** 拉黑天数 */
    public static final int BLACKLIST_DAYS = 7;

    private final ViolationMapper violationMapper;
    private final UserMapper userMapper;
    private final UserSnapshotStore snapshotStore;

    public ViolationService(ViolationMapper violationMapper,
                            UserMapper userMapper,
                            UserSnapshotStore snapshotStore) {
        this.violationMapper = violationMapper;
        this.userMapper = userMapper;
        this.snapshotStore = snapshotStore;
    }

    /** 记一条违约；够次数就把人拉黑，并让他手上的 token 立刻失效 */
    @Transactional
    public void record(Long userId, Long bookingId, String type) {
        violationMapper.insert(userId, bookingId, type);
        userMapper.increaseViolationCount(userId);
        Integer count = userMapper.findViolationCount(userId);
        log.warn("用户 {} 违约 +1（类型 {}，预约 {}），累计 {} 次", userId, type, bookingId, count);

        if (count != null && count >= MAX_BEFORE_BLACKLIST) {
            LocalDateTime until = LocalDateTime.now().plusDays(BLACKLIST_DAYS);
            // 拉黑时把计数清零：否则第 4 次违约时计数还是 4，每违约一次都要重新写一遍拉黑时间
            userMapper.blacklist(userId, until);
            // 权限快照里带着黑名单状态，脏了必须清掉；旧 token 下次请求直接 401，重新登录才是新状态
            snapshotStore.evict(userId);
            log.warn("用户 {} 累计违约 {} 次，已拉黑至 {}", userId, count, until);
        }
    }
}