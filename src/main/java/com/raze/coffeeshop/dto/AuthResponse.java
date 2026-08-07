package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserRole role
) {
}
