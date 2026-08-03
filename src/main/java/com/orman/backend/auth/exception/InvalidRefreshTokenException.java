package com.orman.backend.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("La sesión no es válida o ha expirado.");
    }
}
