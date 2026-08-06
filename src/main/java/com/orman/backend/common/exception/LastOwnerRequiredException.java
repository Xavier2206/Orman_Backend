package com.orman.backend.common.exception;

public class LastOwnerRequiredException extends ApplicationException {

    public LastOwnerRequiredException() {
        super("Debe permanecer al menos un propietario activo.");
    }
}
