package com.raze.coffeeshop.dto;

import com.raze.coffeeshop.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentMethod method,
        BigDecimal amount,
        OffsetDateTime paymentDate
) {
}
