package com.orman.backend.common.exception;

/**
 * Base exception for expected application-level failures.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }
}
