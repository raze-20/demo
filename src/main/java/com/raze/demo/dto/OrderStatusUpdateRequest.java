package com.raze.demo.dto;

import com.raze.demo.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull
        OrderStatus status
) {
}
