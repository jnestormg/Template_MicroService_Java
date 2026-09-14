package com.example.api.user.mapper;

import com.example.api.user.dto.UserResponse;
import com.example.api.user.model.Role;
import com.example.api.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(roleNames(user))")
    UserResponse toResponse(User user);

    default Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}