package com.orman.backend.auth.exception;

public class RevokedSessionException extends RuntimeException {

    public RevokedSessionException() {
        super("La sesión fue revocada.");
    }
}
