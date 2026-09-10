package com.orman.backend.property.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UnidadRequest(
        @NotBlank(message = "El nombre de la Unidad es obligatorio.")
        @Size(max = 100, message = "El nombre de la Unidad no puede superar 100 caracteres.")
        String nombre,
        @NotBlank(message = "El tipo de Unidad es obligatorio.")
        @Size(max = 50, message = "El tipo de Unidad no puede superar 50 caracteres.")
        String tipoUnidad,
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
        String descripcion,
        @NotNull(message = "El área es obligatoria.")
        @DecimalMin(value = "0.00", message = "El área no puede ser negativa.")
        BigDecimal area,
        @NotNull(message = "La cantidad de dormitorios es obligatoria.")
        @Min(value = 0, message = "La cantidad de dormitorios no puede ser negativa.")
        Short dormitorios,
        @NotNull(message = "La cantidad de baños es obligatoria.")
        @Min(value = 0, message = "La cantidad de baños no puede ser negativa.")
        Short banos,
        @NotNull(message = "El piso es obligatorio.")
        @Min(value = 0, message = "El piso no puede ser negativo.")
        Integer piso,
        @Size(max = 150, message = "La ubicación interna no puede superar 150 caracteres.")
        String ubicacionInterna,
        @NotNull(message = "El precio base es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio base no puede ser negativo.")
        BigDecimal precioBase,
        @NotNull(message = "El estado operativo es obligatorio.")
        @Min(value = 0, message = "El estado operativo debe ser 0 o 1.")
        @Max(value = 1, message = "El estado operativo debe ser 0 o 1.")
        Short estadoOperativo) {
}
