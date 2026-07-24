package com.raze.demo.dto;

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
