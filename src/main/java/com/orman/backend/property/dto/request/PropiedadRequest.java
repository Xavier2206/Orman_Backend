package com.orman.backend.property.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PropiedadRequest(
        @NotBlank(message = "El nombre de la Propiedad es obligatorio.")
        @Size(max = 120, message = "El nombre de la Propiedad no puede superar 120 caracteres.")
        String nombre,
        @NotBlank(message = "El tipo de Propiedad es obligatorio.")
        @Pattern(regexp = "(?i)CASA|EDIFICIO", message = "El tipo de Propiedad debe ser CASA o EDIFICIO.")
        String tipo,
        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres.")
        String direccion,
        @NotBlank(message = "La ciudad es obligatoria.")
        @Size(max = 100, message = "La ciudad no puede superar 100 caracteres.")
        String ciudad,
        @Size(max = 255, message = "La referencia no puede superar 255 caracteres.")
        String referencia,
        @DecimalMin(value = "-90.000000", message = "La latitud debe estar entre -90 y 90.")
        @DecimalMax(value = "90.000000", message = "La latitud debe estar entre -90 y 90.")
        BigDecimal latitud,
        @DecimalMin(value = "-180.000000", message = "La longitud debe estar entre -180 y 180.")
        @DecimalMax(value = "180.000000", message = "La longitud debe estar entre -180 y 180.")
        BigDecimal longitud,
        @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "La portada debe ser una URL HTTP o HTTPS válida.")
        @Size(max = 500, message = "La URL de portada no puede superar 500 caracteres.")
        String portadaUrl,
        @NotNull(message = "La Persona propietaria es obligatoria.")
        Integer codperPropietaria,
        @NotNull(message = "La inversión inicial es obligatoria.")
        @DecimalMin(value = "0.00", message = "La inversión inicial no puede ser negativa.")
        BigDecimal inversionInicial,
        @NotNull(message = "El estado es obligatorio.")
        @Min(value = 0, message = "El estado debe ser 0 o 1.")
        @Max(value = 1, message = "El estado debe ser 0 o 1.")
        Short estado) {
}
