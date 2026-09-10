package com.orman.backend.payment.dto.request;

import com.orman.backend.payment.entity.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoRequest(
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.") BigDecimal monto,
        @NotNull MetodoPago metodo,
        Integer codcta,
        @Size(max = 100, message = "La referencia externa no puede superar 100 caracteres.") String referenciaExterna,
        @NotNull LocalDateTime fechaPago,
        @NotNull UUID idempotencyKey) {
}
