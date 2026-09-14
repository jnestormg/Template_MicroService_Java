package com.example.api.common.exception;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message, org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}