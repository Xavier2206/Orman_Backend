package com.orman.backend.common.exception;

public final class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
