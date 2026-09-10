package com.orman.backend.person.exception;

public class InvalidPersonaFilterException extends RuntimeException {

    private final String field;

    public InvalidPersonaFilterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
