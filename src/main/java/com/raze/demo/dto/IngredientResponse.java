package com.raze.demo.dto;

import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        String measureUnit
) {
}
