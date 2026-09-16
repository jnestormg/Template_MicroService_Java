package com.example.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "Nuevos roles a asignar al usuario (reemplazo total)")
public record UpdateUserRolesRequest(
        @Schema(description = "Nombres de roles existentes. La lista sustituye por completo a la actual",
                example = "[\\\"ADMIN\\\", \\\"USER\\\"]", allowableValues = {"ADMIN", "USER"})
        @NotNull(message = "La lista de roles es obligatoria")
        @NotEmpty(message = "Debe indicar al menos un rol")
        Set<String> roles
) {
}