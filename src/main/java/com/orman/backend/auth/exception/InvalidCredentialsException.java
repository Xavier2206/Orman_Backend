package com.orman.backend.auth.exception;

import com.orman.backend.common.exception.ApplicationException;

public final class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super("Las credenciales no son válidas.");
    }
}
