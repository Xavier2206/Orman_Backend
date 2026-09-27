package com.orman.backend.payment.exception;

public class InvalidPaymentImageException extends RuntimeException {

    public InvalidPaymentImageException(String message) {
        super(message);
    }

    public InvalidPaymentImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
