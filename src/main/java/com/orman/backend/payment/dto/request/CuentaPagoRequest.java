package com.orman.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CuentaPagoRequest(
        @NotBlank(message = "El banco es obligatorio.")
        @Size(max = 100, message = "El banco no puede superar 100 caracteres.") String banco,
        @NotBlank(message = "El número de cuenta es obligatorio.")
        @Size(max = 100, message = "El número de cuenta no puede superar 100 caracteres.") String numeroCuenta,
        @NotBlank(message = "El titular es obligatorio.")
        @Size(max = 200, message = "El titular no puede superar 200 caracteres.") String titular,
        @Size(max = 500, message = "La URL QR no puede superar 500 caracteres.")
        @Pattern(regexp = "^$|https?://.+", message = "La URL QR debe usar HTTP o HTTPS.") String qrUrl,
        @Size(max = 1000, message = "Las instrucciones no pueden superar 1000 caracteres.") String instrucciones,
        @NotNull @PositiveOrZero Integer orden,
        @NotNull @Pattern(regexp = "0|1", message = "El estado debe ser 0 o 1.") String estado) {
}
