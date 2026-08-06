package com.raze.demo.dto;

import com.raze.demo.enums.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserRole role
) {
}
