package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull
        OrderStatus status
) {
}
