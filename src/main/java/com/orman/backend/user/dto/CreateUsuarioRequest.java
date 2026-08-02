package com.orman.backend.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUsuarioRequest(
        @NotBlank(message = "El login es obligatorio.")
        @Size(max = 30, message = "El login no puede superar 30 caracteres.")
        String login,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String password,
        @Min(value = 0, message = "El estado debe ser 0 o 1.")
        @Max(value = 1, message = "El estado debe ser 0 o 1.")
        Short estado,
        @NotNull(message = "La Persona es obligatoria.")
        @Positive(message = "El código de Persona debe ser positivo.")
        Integer codper) {
}
