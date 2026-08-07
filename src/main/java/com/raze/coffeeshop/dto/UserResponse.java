package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        Boolean active,
        OffsetDateTime createdAt
) {
}
