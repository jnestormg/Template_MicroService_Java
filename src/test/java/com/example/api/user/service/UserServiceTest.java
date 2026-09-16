package com.example.api.user.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.security.model.AppUserDetails;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Role role(String name) {
        return Role.builder().name(name).build();
    }

    private User buildUser(Long id, Set<Role> roles) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@api.local")
                .password("$2y$10$hashed")
                .enabled(true)
                .roles(roles)
                .build();
    }

    private UserResponse buildResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                Instant.ofEpochMilli(1000));
    }

    @Test
    void createUser_savesWithResolvedRoles() {
        CreateUserRequest request = new CreateUserRequest("juan", "juan@api.local", "password123", Set.of("ADMIN"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByUsername("juan")).thenReturn(false);
        when(userRepository.existsByEmail("juan@api.local")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role("ADMIN")));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(1L, "juan", "juan@api.local", true, Set.of("ADMIN"), Instant.ofEpochMilli(1000)));

        UserResponse result = userService.createUser(request);

        assertThat(result.username()).isEqualTo("juan");
        assertThat(captor.getValue().getRoles()).extracting(Role::getName).containsExactly("ADMIN");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    void createUser_defaultsToUserRoleWhenNoRoles() {
        CreateUserRequest request = new CreateUserRequest("ana", "ana@api.local", "password123", null);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByUsername("ana")).thenReturn(false);
        when(userRepository.existsByEmail("ana@api.local")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(1L, "ana", "ana@api.local", true, Set.of("USER"), Instant.ofEpochMilli(1000)));

        userService.createUser(request);

        assertThat(captor.getValue().getRoles()).extracting(Role::getName).containsExactly("USER");
    }

    @Test
    void createUser_rejectsExistingUsername() {
        CreateUserRequest request = new CreateUserRequest("juan", "juan@api.local", "password123", Set.of());
        when(userRepository.existsByUsername("juan")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(409));
    }

    @Test
    void createUser_rejectsExistingEmail() {
        CreateUserRequest request = new CreateUserRequest("juan", "juan@api.local", "password123", Set.of());
        when(userRepository.existsByUsername("juan")).thenReturn(false);
        when(userRepository.existsByEmail("juan@api.local")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(409));
    }

    @Test
    void createUser_rejectsUnknownRole() {
        CreateUserRequest request = new CreateUserRequest("juan", "juan@api.local", "password123", Set.of("SUPER"));
        when(userRepository.existsByUsername("juan")).thenReturn(false);
        when(userRepository.existsByEmail("juan@api.local")).thenReturn(false);
        when(roleRepository.findByName("SUPER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void updateRoles_replacesRoles() {
        User user = buildUser(1L, Set.of(role("ADMIN")));
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(Set.of("USER"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.countByRolesName("ADMIN")).thenReturn(5L);
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(99L, Set.of(role("ADMIN")))));
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(buildResponse(user));

        UserResponse result = userService.updateRoles(1L, request);

        assertThat(result.id()).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
        assertThat(captor.getValue().getRoles()).extracting(Role::getName).containsExactly("USER");
    }

    @Test
    void updateRoles_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRoles(99L, new UpdateUserRolesRequest(Set.of("USER"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(404));
    }

    @Test
    void updateRoles_rejectsSelfDemotion() {
        User user = buildUser(1L, Set.of(role("ADMIN")));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.countByRolesName("ADMIN")).thenReturn(5L);
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(1L, Set.of(role("ADMIN")))));

        assertThatThrownBy(() -> userService.updateRoles(1L, new UpdateUserRolesRequest(Set.of("USER"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void updateRoles_rejectsRemovingLastAdmin() {
        User user = buildUser(1L, Set.of(role("ADMIN")));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role("USER")));
        when(userRepository.countByRolesName("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRoles(1L, new UpdateUserRolesRequest(Set.of("USER"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void updateUser_updatesProvidedFields() {
        User user = buildUser(1L, Set.of(role("USER")));
        UpdateUserRequest request = new UpdateUserRequest("nuevo", "nuevo@api.local", "newPassword123", null);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("nuevo", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("nuevo@api.local", 1L)).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("nueva-hash");
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(buildResponse(user));

        UserResponse result = userService.updateUser(1L, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(captor.getValue().getUsername()).isEqualTo("nuevo");
        assertThat(captor.getValue().getEmail()).isEqualTo("nuevo@api.local");
        assertThat(captor.getValue().getPassword()).isEqualTo("nueva-hash");
    }

    @Test
    void updateUser_rejectsUsernameTakenByOther() {
        User user = buildUser(1L, Set.of(role("USER")));
        UpdateUserRequest request = new UpdateUserRequest("duplicado", null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("duplicado", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(409));
    }

    @Test
    void updateUser_rejectsDisablingSelf() {
        User user = buildUser(1L, Set.of(role("ADMIN")));
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(1L, Set.of(role("ADMIN")))));

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void updateUser_rejectsDisablingLastAdmin() {
        User user = buildUser(1L, Set.of(role("ADMIN")));
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(99L, Set.of(role("ADMIN")))));
        when(userRepository.countByRolesName("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void updateUser_reenablesDisabledUser() {
        User user = buildUser(1L, Set.of(role("USER")));
        user.setEnabled(false);
        UpdateUserRequest request = new UpdateUserRequest(null, null, null, true);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(buildResponse(user));

        userService.updateUser(1L, request);

        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    void deleteUser_disablesUser() {
        User user = buildUser(1L, Set.of(role("USER")));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(99L, Set.of(role("USER")))));
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteUser(1L);

        assertThat(captor.getValue().isEnabled()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_isIdempotentWhenAlreadyDisabled() {
        User user = buildUser(1L, Set.of(role("USER")));
        user.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void deleteUser_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(404));
    }

    @Test
    void deleteUser_rejectsSelfDisable() {
        User user = buildUser(1L, Set.of(role("ADMIN")));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(1L, Set.of(role("ADMIN")))));

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }

    @Test
    void deleteUser_rejectsLastAdmin() {
        User user = buildUser(1L, Set.of(role("ADMIN")));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUser()).thenReturn(AppUserDetails.from(buildUser(99L, Set.of(role("ADMIN")))));
        when(userRepository.countByRolesName("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus().value()).isEqualTo(400));
    }
}