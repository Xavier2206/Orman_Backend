package com.orman.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PagoComprobanteRequest(
        @NotBlank(message = "La URL es obligatoria.")
        @Size(max = 500, message = "La URL no puede superar 500 caracteres.")
        @Pattern(regexp = "https?://.+", message = "La URL debe usar HTTP o HTTPS.") String url,
        @NotBlank(message = "El nombre del archivo es obligatorio.")
        @Size(max = 255, message = "El nombre del archivo no puede superar 255 caracteres.") String nombreArchivo,
        @NotBlank(message = "El tipo de contenido es obligatorio.")
        @Size(max = 100, message = "El tipo de contenido no puede superar 100 caracteres.") String tipoContenido,
        @NotNull @PositiveOrZero Integer orden) {
}
