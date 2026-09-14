package com.example.api.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo admite letras, numeros, puntos, guiones y guion bajo")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email con formato invalido")
        @Size(max = 120, message = "Email demasiado largo")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 60, message = "La contrasena debe tener entre 8 y 60 caracteres")
        String password
) {
}