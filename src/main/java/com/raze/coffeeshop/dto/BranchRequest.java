package com.raze.coffeeshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank String address,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Size(max = 50) String state
) {
}
