package com.seat.reserve.web.dto;

import com.seat.reserve.security.UserSnapshot;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSnapshot user
) {
}