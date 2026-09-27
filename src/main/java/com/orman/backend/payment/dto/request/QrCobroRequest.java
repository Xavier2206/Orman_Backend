package com.orman.backend.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record QrCobroRequest(
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin) {
}
