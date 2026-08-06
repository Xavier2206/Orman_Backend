package com.orman.backend.auth.exception;

public class SecurityAccessDeniedException extends RuntimeException {

    public SecurityAccessDeniedException() {
        super("La solicitud de seguridad no es válida.");
    }
}
