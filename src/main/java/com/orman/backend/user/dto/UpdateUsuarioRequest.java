package com.orman.backend.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateUsuarioRequest(
        @NotNull(message = "El estado es obligatorio.")
        @Min(value = 0, message = "El estado debe ser 0 o 1.")
        @Max(value = 1, message = "El estado debe ser 0 o 1.")
        Short estado) {
}
