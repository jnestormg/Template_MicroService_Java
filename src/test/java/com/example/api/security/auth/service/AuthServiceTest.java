package com.example.api.security.auth.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.security.auth.dto.AuthResponse;
import com.example.api.security.auth.dto.LoginRequest;
import com.example.api.security.auth.dto.LogoutRequest;
import com.example.api.security.auth.dto.RefreshRequest;
import com.example.api.security.auth.dto.RegisterRequest;
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
import com.example.api.user.model.Permission;
import com.example.api.user.model.Role;
import com.example.api.user.model.User;
import com.example.api.user.repository.RoleRepository;
import com.example.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthService authService;

    private User buildUser() {
        Permission read = Permission.builder().name("PRODUCT:READ").build();
        Role role = Role.builder().name("USER").permissions(Set.of(read)).build();
        return User.builder()
                .id(1L)
                .username("admin")
                .email("admin@api.local")
                .password("$2y$10$hashed")
                .enabled(true)
                .roles(Set.of(role))
                .build();
    }

    private UserResponse buildResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.isEnabled(), Set.of("USER"), Instant.ofEpochMilli(1000));
    }

    private void stubTokenIssuance(User user) {
        when(jwtService.generateTokenPair(any(AppUserDetails.class)))
                .thenReturn(new TokenPair("access-token", "refresh-token"));
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(userMapper.toResponse(user)).thenReturn(buildResponse(user));
    }

    @Test
    void register_createsUserWithDefaultRoleAndIssuesTokens() {
        RegisterRequest request = new RegisterRequest("admin", "admin@api.local", "password123");
        User user = buildUser();

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@api.local")).thenReturn(false);
        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(Role.builder().name("USER").permissions(Set.of()).build()));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = buildResponse(user);
        when(jwtService.generateTokenPair(any(AppUserDetails.class)))
                .thenReturn(new TokenPair("access-token", "refresh-token"));
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        AuthResponse result = authService.register(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.user().username()).isEqualTo("admin");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_rejectsExistingUsername() {
        RegisterRequest request = new RegisterRequest("admin", "admin@api.local", "password123");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(409));
    }

    @Test
    void login_issuesTokensOnValidCredentials() {
        LoginRequest request = new LoginRequest("admin", "password123");
        User user = buildUser();
        stubTokenIssuance(user);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

        AuthResponse result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(jwtService).generateTokenPair(any(AppUserDetails.class));
    }

    @Test
    void login_rejectsInvalidPassword() {
        LoginRequest request = new LoginRequest("admin", "wrong");
        User user = buildUser();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(401));

        verify(jwtService, never()).generateTokenPair(any(AppUserDetails.class));
    }

    @Test
    void refresh_rotatesRefreshToken() {
        User user = buildUser();
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("ajduwu")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        RefreshRequest request = new RefreshRequest("refresh-token");

        when(jwtService.parse("refresh-token", TokenType.REFRESH))
                .thenReturn(new JwtClaims("admin", 1L, TokenType.REFRESH, List.of(), "jti"));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        stubTokenIssuance(user);

        AuthResponse result = authService.refresh(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void refresh_rejectsRevokedToken() {
        User user = buildUser();
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("ajduwu")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(jwtService.parse("refresh-token", TokenType.REFRESH))
                .thenReturn(new JwtClaims("admin", 1L, TokenType.REFRESH, List.of(), "jti"));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-token")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(401));
    }

    @Test
    void logout_revokesTokenBelongingToCurrentUser() {
        User user = buildUser();
        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("ajduwu")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        AppUserDetails current = AppUserDetails.from(user);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(currentUserService.getCurrentUser()).thenReturn(current);

        authService.logout(new LogoutRequest("refresh-token"));

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }
}