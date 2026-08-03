package com.orman.backend.auth.exception;

public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException() {
        super("JWT inválido.");
    }
}
