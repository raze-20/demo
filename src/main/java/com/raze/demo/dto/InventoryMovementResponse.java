package com.raze.demo.dto;

import com.raze.demo.enums.MovementType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID ingredientId,
        MovementType type,
        BigDecimal quantity,
        String reason,
        UUID performedByUserId,
        OffsetDateTime createdAt
) {
}
