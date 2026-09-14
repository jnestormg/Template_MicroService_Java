package com.example.api.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        Set<String> roles,
        Instant createdAt
) {
}