package com.example.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos a actualizar de un usuario. Solo se aplican los campos enviados")
public record UpdateUserRequest(
        @Schema(description = "Nuevo nombre de usuario", example = "juan", minLength = 3, maxLength = 50)
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El usuario solo admite letras, numeros, puntos, guiones y guion bajo")
        String username,

        @Schema(description = "Nuevo email", example = "nuevo@api.local")
        @Email(message = "Email con formato invalido")
        @Size(max = 120, message = "Email demasiado largo")
        String email,

        @Schema(description = "Nueva contrasena (en texto plano)", example = "nuevaPassword123", minLength = 8, maxLength = 60)
        @Size(min = 8, max = 60, message = "La contrasena debe tener entre 8 y 60 caracteres")
        String password,

        @Schema(description = "Habilitar (true) o deshabilitar (false) la cuenta. Deshabilitar impide iniciar sesion", example = "true")
        Boolean enabled
) {
}