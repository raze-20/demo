package com.raze.coffeeshop.dto;

import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        String measureUnit
) {
}
