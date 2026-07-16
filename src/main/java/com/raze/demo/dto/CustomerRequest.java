package com.raze.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CustomerRequest(
        @NotNull UUID userId,
        @Min(0) Integer loyaltyPoints,
        LocalDate birthDate
) {
}
