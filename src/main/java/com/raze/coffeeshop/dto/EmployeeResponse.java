package com.raze.coffeeshop.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UUID branchId,
        String branchName,
        String position,
        String role,
        LocalDate hireDate
) {
}
