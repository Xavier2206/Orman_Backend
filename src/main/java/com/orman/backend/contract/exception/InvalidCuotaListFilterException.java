package com.orman.backend.contract.exception;

public class InvalidCuotaListFilterException extends RuntimeException {

    private final String field;

    public InvalidCuotaListFilterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
