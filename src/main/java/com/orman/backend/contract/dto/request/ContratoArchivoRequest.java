package com.orman.backend.contract.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContratoArchivoRequest(
        @NotBlank(message = "La URL del archivo es obligatoria.")
        @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "El archivo debe ser una URL HTTP o HTTPS válida.")
        @Size(max = 500, message = "La URL del archivo no puede superar 500 caracteres.")
        String url,
        @NotBlank(message = "El nombre del archivo es obligatorio.")
        @Size(max = 200, message = "El nombre del archivo no puede superar 200 caracteres.")
        String nombreArchivo,
        @Size(max = 100, message = "El tipo de contenido no puede superar 100 caracteres.")
        String tipoContenido,
        @NotNull(message = "El orden es obligatorio.")
        @Min(value = 0, message = "El orden no puede ser negativo.")
        Integer orden) {
}
