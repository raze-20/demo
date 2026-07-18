package com.raze.demo.dto;

import com.raze.demo.enums.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeUpdateRequest(
        @NotNull UserRole type,
        @NotNull UUID branchId,
        @NotBlank @Size(max = 100) String position,
        @NotNull LocalDate hireDate
) {
}
