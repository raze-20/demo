package com.raze.demo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        BigDecimal basePrice,
        Boolean active,
        Integer categoryId,
        String categoryName
) {
}
