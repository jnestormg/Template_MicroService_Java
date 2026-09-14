package com.example.api.security.token;

import com.example.api.common.exception.InvalidTokenException;
import com.example.api.security.config.JwtProperties;
import com.example.api.security.model.AppUserDetails;
import com.example.api.user.model.Permission;
import com.example.api.user.model.Role;
import com.example.api.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder()
                .encodeToString("clave-secreta-de-prueba-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8)));
        properties.setAccessTokenExpirationMs(900_000L);
        properties.setRefreshTokenExpirationMs(604_800_000L);
        properties.setIssuer("api");
        jwtService = new JwtService(properties);
    }

    private AppUserDetails buildUser() {
        Permission read = Permission.builder().name("PRODUCT:READ").build();
        Role role = Role.builder().name("USER").permissions(Set.of(read)).build();
        User user = User.builder()
                .id(1L)
                .username("admin")
                .password("hashed")
                .enabled(true)
                .roles(Set.of(role))
                .build();
        return AppUserDetails.from(user);
    }

    @Test
    void generateTokenPair_createsAccessAndRefreshTokens() {
        TokenPair pair = jwtService.generateTokenPair(buildUser());

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();

        JwtClaims access = jwtService.parse(pair.accessToken(), TokenType.ACCESS);
        JwtClaims refresh = jwtService.parse(pair.refreshToken(), TokenType.REFRESH);

        assertThat(access.subject()).isEqualTo("admin");
        assertThat(access.userId()).isEqualTo(1L);
        assertThat(access.authorities()).contains("ROLE_USER", "PRODUCT:READ");
        assertThat(refresh.subject()).isEqualTo("admin");
    }

    @Test
    void parse_rejectsRefreshTokenAsAccess() {
        TokenPair pair = jwtService.generateTokenPair(buildUser());

        assertThatThrownBy(() -> jwtService.parse(pair.refreshToken(), TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parse_rejectsExpiredToken() {
        String expired = jwtService.generateToken(buildUser(), TokenType.ACCESS, -1_000L);

        assertThatThrownBy(() -> jwtService.parse(expired, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parse_rejectsTokenWithInvalidSignature() {
        String token = jwtService.generateToken(buildUser(), TokenType.ACCESS, 900_000L);
        String tampered = token.substring(0, token.length() - 2) + "ab";

        assertThatThrownBy(() -> jwtService.parse(tampered, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }
}