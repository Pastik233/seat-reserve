package com.seat.reserve.web;

import com.seat.reserve.common.R;
import com.seat.reserve.security.UserSnapshot;
import com.seat.reserve.service.AuthService;
import com.seat.reserve.web.dto.LoginRequest;
import com.seat.reserve.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    /** 当前用户：数据来自 Redis 快照，不查库 */
    @GetMapping("/me")
    public R<UserSnapshot> me(@AuthenticationPrincipal UserSnapshot me) {
        return R.ok(me);
    }
}