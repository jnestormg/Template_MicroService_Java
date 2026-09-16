package com.example.api.user.controller;

import com.example.api.common.web.ApiResponse;
import com.example.api.common.web.PageResponse;
import com.example.api.security.model.AppUserDetails;
import com.example.api.security.service.CurrentUserService;
import com.example.api.user.dto.CreateUserRequest;
import com.example.api.user.dto.UpdateUserRequest;
import com.example.api.user.dto.UpdateUserRolesRequest;
import com.example.api.user.dto.UserResponse;
import com.example.api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios", description = "Consulta y administracion de usuarios")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        AppUserDetails current = currentUserService.getCurrentUser();
        return ApiResponse.ok(userService.getUserById(current.getUserId()));
    }

    @Operation(summary = "Crear un usuario y asignarle roles",
            description = "Requiere el permiso USER:CREATE. Si roles se omite o llega vacio, se asigna USER por defecto.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario creado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos o rol inexistente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permiso USER:CREATE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username o email ya registrados")
    })
    @PreAuthorize("hasAuthority('USER:CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("Usuario creado", userService.createUser(request));
    }

    @Operation(summary = "Reemplazar los roles de un usuario",
            description = "Requiere el permiso USER:UPDATE. La lista enviada sustituye a la actual. "
                    + "No se permite auto-revocar el rol ADMIN ni dejar al sistema sin administradores.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles actualizados"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos, rol inexistente, auto-revocar ADMIN o ultimo administrador"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permiso USER:UPDATE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> updateUserRoles(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.ok("Roles actualizados", userService.updateRoles(id, request));
    }

    @Operation(summary = "Actualizar datos de un usuario",
            description = "Requiere el permiso USER:UPDATE. Solo se actualizan los campos enviados. "
                    + "Deshabilitar la propia cuenta o al ultimo administrador no esta permitido.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos, deshabilitar self o ultimo administrador"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permiso USER:UPDATE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username o email ya registrados por otro usuario")
    })
    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id,
                                                @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("Usuario actualizado", userService.updateUser(id, request));
    }

    @Operation(summary = "Borrado logico de un usuario",
            description = "Requiere el permiso USER:UPDATE. Marca enabled=false; el usuario ya no puede iniciar sesion. "
                    + "No se permite deshabilitar la propia cuenta ni al ultimo administrador.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Usuario deshabilitado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Deshabilitar self o ultimo administrador"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permiso USER:UPDATE"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PreAuthorize("hasAuthority('USER:UPDATE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(userService.listUsers(pageable));
    }
}