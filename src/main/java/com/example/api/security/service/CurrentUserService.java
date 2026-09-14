package com.example.api.security.service;

import com.example.api.common.exception.BusinessException;
import com.example.api.security.model.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AppUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails userDetails)) {
            throw new BusinessException("UNAUTHENTICATED", "No autenticado", HttpStatus.UNAUTHORIZED);
        }
        return userDetails;
    }
}