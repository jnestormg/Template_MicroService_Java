package com.example.api.user.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.common.web.PageResponse;
import com.example.api.user.dto.UserResponse;
import com.example.api.user.mapper.UserMapper;
import com.example.api.user.model.User;
import com.example.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado"));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(userMapper::toResponse);
        return PageResponse.from(page);
    }
}