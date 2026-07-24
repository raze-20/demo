package com.raze.demo.dto;

import com.raze.demo.enums.PaymentMethod;

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
