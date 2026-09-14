package com.example.api.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        Instant createdAt,
        Instant updatedAt
) {
}