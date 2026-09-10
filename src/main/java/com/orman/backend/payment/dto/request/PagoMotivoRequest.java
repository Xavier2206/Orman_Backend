package com.orman.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PagoMotivoRequest(
        @NotBlank(message = "El motivo es obligatorio.")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres.") String motivo) {
}
