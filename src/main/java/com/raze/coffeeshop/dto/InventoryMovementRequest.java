package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.MovementType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryMovementRequest(
        @NotNull
        UUID ingredientId,

        @NotNull
        MovementType type,

        @NotNull
        @DecimalMin(value = "0.001")
        BigDecimal quantity,

        @Size(max = 255)
        String reason
) {
}
