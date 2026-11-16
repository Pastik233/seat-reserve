package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.SysUser;
import com.seat.reserve.mapper.UserMapper;
import com.seat.reserve.security.JwtUtil;
import com.seat.reserve.security.UserSnapshot;
import com.seat.reserve.security.UserSnapshotStore;
import com.seat.reserve.web.dto.LoginRequest;
import com.seat.reserve.web.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserSnapshotStore snapshotStore;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       UserSnapshotStore snapshotStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.snapshotStore = snapshotStore;
    }

    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.findByUsername(req.username());
        // 用户不存在和密码错误给同一句话，防止用户名枚举
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        LocalDateTime blacklistUntil = user.getBlacklistUntil();
        if (blacklistUntil != null && blacklistUntil.isAfter(LocalDateTime.now())) {
            throw BizException.forbidden("账号已被拉黑，解禁时间 " + blacklistUntil.toLocalDate());
        }

        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRoleCode());
        UserSnapshot snapshot = new UserSnapshot(
                user.getId(), user.getUsername(), user.getNickname(), user.getRoleCode(),
                blacklistUntil == null ? null : blacklistUntil.toString());
        snapshotStore.save(snapshot, Duration.ofMillis(jwtUtil.getExpireMillis()));
        return new LoginResponse(token, "Bearer", jwtUtil.getExpireMillis() / 1000, snapshot);
    }
}