package com.orman.backend.role.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRolRequest(
        @NotBlank(message = "El nombre del Rol es obligatorio.")
        @Size(max = 50, message = "El nombre del Rol no puede superar 50 caracteres.")
        String nombre,
        @Min(value = 0, message = "El estado debe ser 0 o 1.")
        @Max(value = 1, message = "El estado debe ser 0 o 1.")
        Short estado) {
}
