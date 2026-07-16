package com.raze.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(
        @NotNull UUID userId,
        @NotNull UUID branchId,
        @NotBlank @Size(max = 100) String position,
        @NotBlank @Size(max = 100) String role,
        @NotNull LocalDate hireDate
) {
}
