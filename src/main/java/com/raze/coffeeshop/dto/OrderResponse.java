package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID branchId,
        String branchName,
        UUID employeeId,
        UUID customerId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal taxes,
        BigDecimal total,
        BigDecimal balanceDue,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items,
        List<PaymentResponse> payments
) {
}
