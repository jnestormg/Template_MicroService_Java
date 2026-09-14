package com.example.api.security.token;

import java.util.List;

public record JwtClaims(
        String subject,
        Long userId,
        TokenType type,
        List<String> authorities,
        String jwtId
) {
}