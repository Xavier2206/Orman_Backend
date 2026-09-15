package com.orman.backend.contract.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRequest(
        @NotNull(message = "El inquilino es obligatorio.")
        Integer codperInquilino,
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria.")
        LocalDate fechaFin,
        @NotNull(message = "El monto mensual es obligatorio.")
        @DecimalMin(value = "0.01", message = "El monto mensual debe ser mayor a cero.")
        @Digits(integer = 12, fraction = 2, message = "El monto mensual debe tener máximo dos decimales.")
        BigDecimal montoMensual,
        @NotNull(message = "La garantía es obligatoria.")
        @DecimalMin(value = "0.00", message = "La garantía no puede ser negativa.")
        @Digits(integer = 12, fraction = 2, message = "La garantía debe tener máximo dos decimales.")
        BigDecimal garantia) {
}
