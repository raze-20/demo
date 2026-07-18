package com.raze.demo.dto;

import com.raze.demo.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos para registrar un empleado nuevo: crea el {@code User} (con el rol indicado en
 * {@code type}) y su perfil de {@code Employee} en un solo paso. {@code type} debe ser un
 * rol operativo ({@code ADMIN}, {@code MANAGER}, {@code CASHIER} o {@code BARISTA}); el
 * servicio rechaza {@code CUSTOMER} aquí porque esa alta se hace vía {@code /api/customers}.
 */
public record EmployeeRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull UserRole type,
        @NotNull UUID branchId,
        @NotBlank @Size(max = 100) String position,
        @NotNull LocalDate hireDate
) {
}
