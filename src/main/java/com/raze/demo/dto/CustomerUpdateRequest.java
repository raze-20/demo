package com.raze.demo.dto;

import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record CustomerUpdateRequest(
        @Min(0) Integer loyaltyPoints,
        LocalDate birthDate
) {
}
