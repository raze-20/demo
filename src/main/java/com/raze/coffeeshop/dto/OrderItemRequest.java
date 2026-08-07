package com.raze.coffeeshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull
        UUID productId,

        @NotNull
        @Min(1)
        @Max(500)
        Integer quantity,

        @Size(max = 255)
        String notes
) {
}
