package com.orman.backend.contract.exception;

public class InvalidContratoArchivoException extends RuntimeException {

    public InvalidContratoArchivoException(String message) {
        super(message);
    }

    public InvalidContratoArchivoException(String message, Throwable cause) {
        super(message, cause);
    }
}
