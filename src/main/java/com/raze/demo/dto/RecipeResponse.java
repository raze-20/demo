package com.raze.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeResponse(
        UUID productId,
        UUID ingredientId,
        String ingredientName,
        String measureUnit,
        BigDecimal requiredQuantity
) {
}
