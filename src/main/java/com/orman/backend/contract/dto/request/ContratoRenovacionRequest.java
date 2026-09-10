package com.orman.backend.contract.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRenovacionRequest(
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria.")
        LocalDate fechaFin,
        @NotNull(message = "El monto mensual es obligatorio.")
        @DecimalMin(value = "0.00", message = "El monto mensual no puede ser negativo.")
        BigDecimal montoMensual,
        @NotNull(message = "La garantía es obligatoria.")
        @DecimalMin(value = "0.00", message = "La garantía no puede ser negativa.")
        BigDecimal garantia) {
}
