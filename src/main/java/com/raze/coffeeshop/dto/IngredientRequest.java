package com.raze.coffeeshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String measureUnit
) {
}
