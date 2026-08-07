package com.raze.coffeeshop.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchInventoryResponse(
        UUID id,
        UUID branchId,
        UUID ingredientId,
        String ingredientName,
        String measureUnit,
        BigDecimal currentQuantity,
        BigDecimal minimumStock,
        OffsetDateTime lastUpdated
) {
}
