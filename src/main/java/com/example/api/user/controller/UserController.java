package com.example.api.user.controller;

import com.example.api.common.web.ApiResponse;
import com.example.api.common.web.PageResponse;
import com.example.api.security.model.AppUserDetails;
import com.example.api.security.service.CurrentUserService;
import com.example.api.user.dto.UserResponse;
import com.example.api.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios", description = "Consulta de usuarios")
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

    @PreAuthorize("hasAuthority('USER:READ')")
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.ok(userService.listUsers(pageable));
    }
}