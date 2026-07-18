package com.raze.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para registrar un cliente nuevo: crea el {@code User} (con rol {@code CUSTOMER})
 * y su perfil de {@code Customer} en un solo paso.
 */
public record CustomerRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Min(0) Integer loyaltyPoints,
        LocalDate birthDate
) {
}
