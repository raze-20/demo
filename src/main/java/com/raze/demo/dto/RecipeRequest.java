package com.raze.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeRequest(
        @NotNull
        UUID ingredientId,

        @NotNull
        @DecimalMin(value = "0.001")
        BigDecimal requiredQuantity
) {
}
