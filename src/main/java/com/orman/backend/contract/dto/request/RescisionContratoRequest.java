package com.orman.backend.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RescisionContratoRequest(
        @NotNull(message = "La fecha de rescisión es obligatoria.")
        LocalDate fechaRescision,
        @NotBlank(message = "El motivo de rescisión es obligatorio.")
        @Size(max = 500, message = "El motivo de rescisión no puede superar 500 caracteres.")
        String motivoRescision) {
}
