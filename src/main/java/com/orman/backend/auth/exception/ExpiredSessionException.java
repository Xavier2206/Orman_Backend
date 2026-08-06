package com.orman.backend.auth.exception;

public class ExpiredSessionException extends RuntimeException {

    public ExpiredSessionException() {
        super("La sesión ha expirado.");
    }
}
