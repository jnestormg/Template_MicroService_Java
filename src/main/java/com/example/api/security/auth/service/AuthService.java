package com.example.api.security.auth.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.security.auth.dto.AuthResponse;
import com.example.api.security.auth.dto.LoginRequest;
import com.example.api.security.auth.dto.LogoutRequest;
import com.example.api.security.auth.dto.RefreshRequest;
import com.example.api.security.auth.model.RefreshToken;
import com.example.api.security.auth.repository.RefreshTokenRepository;
import com.example.api.security.config.JwtProperties;
import com.example.api.security.model.AppUserDetails;
import com.example.api.security.service.CurrentUserService;
import com.example.api.security.token.JwtClaims;
import com.example.api.security.token.JwtService;
import com.example.api.security.token.TokenPair;
import com.example.api.security.token.TokenType;
import com.example.api.user.dto.UserResponse;
import com.example.api.user.mapper.UserMapper;
import com.example.api.user.model.User;
import com.example.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> BusinessException.unauthorized("INVALID_CREDENTIALS", "Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.unauthorized("INVALID_CREDENTIALS", "Credenciales invalidas");
        }
        if (!user.isEnabled()) {
            throw BusinessException.unauthorized("USER_DISABLED", "El usuario esta deshabilitado");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        JwtClaims claims = jwtService.parse(request.token(), TokenType.REFRESH);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> BusinessException.unauthorized("INVALID_REFRESH", "Refresh token invalido"));

        if (stored.isRevoked()) {
            throw BusinessException.unauthorized("REFRESH_REVOKED", "Refresh token revocado");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw BusinessException.unauthorized("REFRESH_EXPIRED", "Refresh token expirado");
        }
        if (!stored.getUser().getUsername().equals(claims.subject())) {
            throw BusinessException.unauthorized("REFRESH_MISMATCH", "Refresh token no coincide con el usuario");
        }
        if (!stored.getUser().isEnabled()) {
            throw BusinessException.unauthorized("USER_DISABLED", "El usuario esta deshabilitado");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> BusinessException.unauthorized("INVALID_REFRESH", "Refresh token invalido"));

        Long currentUserId = currentUserService.getCurrentUser().getUserId();
        if (!stored.getUser().getId().equals(currentUserId)) {
            throw BusinessException.unauthorized("LOGOUT_FORBIDDEN", "Este token no pertenece al usuario autenticado");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
    }

    private AuthResponse issueTokens(User user) {
        AppUserDetails userDetails = AppUserDetails.from(user);
        TokenPair pair = jwtService.generateTokenPair(userDetails);
        persistRefreshToken(pair.refreshToken(), user);

        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthResponse(
                pair.accessToken(),
                pair.refreshToken(),
                "Bearer",
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                userResponse
        );
    }

    private void persistRefreshToken(String rawToken, User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}