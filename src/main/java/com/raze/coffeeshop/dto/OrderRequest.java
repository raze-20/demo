package com.raze.coffeeshop.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderRequest(
        @NotNull
        UUID branchId,

        @NotNull
        UUID employeeId,

        UUID customerId
) {
}
