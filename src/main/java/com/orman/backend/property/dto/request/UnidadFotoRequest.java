package com.orman.backend.property.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadFotoRequest(
        @NotBlank(message = "La URL de la fotografía es obligatoria.")
        @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "La fotografía debe ser una URL HTTP o HTTPS válida.")
        @Size(max = 500, message = "La URL de la fotografía no puede superar 500 caracteres.")
        String url,
        @Size(max = 150, message = "El título no puede superar 150 caracteres.")
        String titulo,
        @Size(max = 100, message = "El ambiente no puede superar 100 caracteres.")
        String ambiente,
        @NotNull(message = "El orden es obligatorio.")
        @Min(value = 0, message = "El orden no puede ser negativo.")
        Integer orden) {
}
