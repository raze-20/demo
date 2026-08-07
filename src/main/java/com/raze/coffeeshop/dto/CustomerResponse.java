package com.raze.coffeeshop.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Integer loyaltyPoints,
        LocalDate birthDate
) {
}
