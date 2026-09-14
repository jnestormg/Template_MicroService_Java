package com.example.api.security.auth.controller;

import com.example.api.common.web.ApiResponse;
import com.example.api.security.auth.dto.AuthResponse;
import com.example.api.security.auth.dto.LoginRequest;
import com.example.api.security.auth.dto.LogoutRequest;
import com.example.api.security.auth.dto.RefreshRequest;
import com.example.api.security.auth.dto.RegisterRequest;
import com.example.api.security.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticacion", description = "Registro, login y gestion de tokens JWT")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar un nuevo usuario")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Usuario registrado", authService.register(request));
    }

    @Operation(summary = "Iniciar sesion y obtener tokens")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Sesion iniciada", authService.login(request));
    }

    @Operation(summary = "Renovar tokens con el refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok("Tokens renovados", authService.refresh(request));
    }

    @Operation(summary = "Cerrar sesion revocando el refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }
}