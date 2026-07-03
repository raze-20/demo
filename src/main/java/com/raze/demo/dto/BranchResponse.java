package com.raze.demo.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchResponse(
    UUID id,
    String name,
    String address,
    String city,
    String state,
    OffsetDateTime createdAt
) {
}
