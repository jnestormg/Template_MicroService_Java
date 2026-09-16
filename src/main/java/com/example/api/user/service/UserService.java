package com.example.api.user.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.common.web.PageResponse;
import com.example.api.security.service.CurrentUserService;
import com.example.api.user.dto.CreateUserRequest;
import com.example.api.user.dto.UpdateUserRequest;
import com.example.api.user.dto.UpdateUserRolesRequest;
import com.example.api.user.dto.UserResponse;
import com.example.api.user.mapper.UserMapper;
import com.example.api.user.model.Role;
import com.example.api.user.model.User;
import com.example.api.user.repository.RoleRepository;
import com.example.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessException.conflict("USERNAME_TAKEN", "El nombre de usuario ya esta en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("EMAIL_TAKEN", "El email ya esta registrado");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(resolveRoles(request.roles()))
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateRoles(Long id, UpdateUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado"));

        Set<Role> newRoles = resolveRoles(request.roles());
        Set<String> newRoleNames = newRoles.stream().map(Role::getName).collect(Collectors.toSet());

        boolean removingAdmin = hasRole(user, ADMIN_ROLE) && !newRoleNames.contains(ADMIN_ROLE);
        if (removingAdmin) {
            if (userRepository.countByRolesName(ADMIN_ROLE) <= 1) {
                throw BusinessException.badRequest("LAST_ADMIN",
                        "No puede quitarse el rol ADMIN al ultimo administrador");
            }
            if (user.getId().equals(currentUserService.getCurrentUser().getUserId())) {
                throw BusinessException.badRequest("SELF_DEMOTION",
                        "No puede quitarse el rol ADMIN a si mismo");
            }
        }

        user.setRoles(newRoles);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
                throw BusinessException.conflict("USERNAME_TAKEN", "El nombre de usuario ya esta en uso");
            }
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw BusinessException.conflict("EMAIL_TAKEN", "El email ya esta registrado");
            }
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.enabled() != null) {
            if (!request.enabled()) {
                assertCanDisable(user);
            }
            user.setEnabled(request.enabled());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado"));

        if (!user.isEnabled()) {
            return;
        }
        assertCanDisable(user);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void assertCanDisable(User user) {
        Long currentUserId = currentUserService.getCurrentUser().getUserId();
        if (user.getId().equals(currentUserId)) {
            throw BusinessException.badRequest("SELF_DISABLE", "No puede deshabilitar su propia cuenta");
        }
        if (hasRole(user, ADMIN_ROLE) && userRepository.countByRolesName(ADMIN_ROLE) <= 1) {
            throw BusinessException.badRequest("LAST_ADMIN",
                    "No puede deshabilitar al ultimo administrador");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(userMapper::toResponse);
        return PageResponse.from(page);
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<String> names = roleNames == null ? Set.of() : roleNames;
        if (names.isEmpty()) {
            return Set.of(findRole(DEFAULT_ROLE));
        }
        return names.stream()
                .map(this::findRole)
                .collect(Collectors.toSet());
    }

    private Role findRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> BusinessException.badRequest("ROLE_NOT_FOUND", "Rol no configurado: " + name));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }
}