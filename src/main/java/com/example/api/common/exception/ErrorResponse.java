package com.example.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.LinkedHashMap;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        int status,
        String message,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(false, status.value(), message, Map.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return new ErrorResponse(false, status.value(), message, errors);
    }
}