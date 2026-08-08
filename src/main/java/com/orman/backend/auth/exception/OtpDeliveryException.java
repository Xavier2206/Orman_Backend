package com.orman.backend.auth.exception;

public class OtpDeliveryException extends RuntimeException {
    public OtpDeliveryException() { super("No fue posible enviar el código de verificación."); }
}
