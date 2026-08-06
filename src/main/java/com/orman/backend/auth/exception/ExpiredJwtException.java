package com.orman.backend.auth.exception;

public class ExpiredJwtException extends RuntimeException {

    public ExpiredJwtException() {
        super("El access token ha expirado.");
    }
}
