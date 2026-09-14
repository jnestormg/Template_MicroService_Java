package com.example.api.product.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductEvent(
        Long id,
        String name,
        BigDecimal price,
        boolean available,
        Instant occurredAt
) {
}