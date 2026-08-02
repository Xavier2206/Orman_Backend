package com.orman.backend.role.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRolRequest(
        @NotBlank(message = "El nombre del Rol es obligatorio.")
        @Size(max = 50, message = "El nombre del Rol no puede superar 50 caracteres.")
        String nombre) {
}
