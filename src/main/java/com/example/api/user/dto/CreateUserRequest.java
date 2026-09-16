package com.example.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Datos para crear un usuario como administrador")
public record CreateUserRequest(
        @Schema(description = "Nombre de usuario unico", example = "juan", minLength = 3, maxLength = 50)
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo admite letras, numeros, puntos, guiones y guion bajo")
        String username,

        @Schema(description = "Correo electronico unico", example = "juan@api.local")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email con formato invalido")
        @Size(max = 120, message = "Email demasiado largo")
        String email,

        @Schema(description = "Contrasena en texto plano", example = "password123", minLength = 8, maxLength = 60)
        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 60, message = "La contrasena debe tener entre 8 y 60 caracteres")
        String password,

        @Schema(description = "Roles a asignar. Si se omite o viene vacia, se asigna USER",
                example = "[\\\"USER\\\"]", allowableValues = {"ADMIN", "USER"})
        Set<String> roles
) {
}