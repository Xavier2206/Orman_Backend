package com.orman.backend.payment.dto.request;

import com.orman.backend.payment.entity.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.")
        @Digits(integer = 12, fraction = 2, message = "El monto debe tener máximo dos decimales.") BigDecimal monto,
        @NotNull MetodoPago metodo,
        LocalDateTime fechaPago,
        @NotNull UUID idempotencyKey) {
}
