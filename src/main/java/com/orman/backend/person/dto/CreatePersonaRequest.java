package com.orman.backend.person.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePersonaRequest(
        @NotBlank(message = "El CI es obligatorio.") @Size(max = 20, message = "El CI no puede superar 20 caracteres.") String ci,
        @NotBlank(message = "El nombre es obligatorio.") @Size(max = 60, message = "El nombre no puede superar 60 caracteres.") String nombre,
        @Size(max = 40, message = "El apellido paterno no puede superar 40 caracteres.") String ap,
        @Size(max = 40, message = "El apellido materno no puede superar 40 caracteres.") String am,
        @NotNull(message = "El género es obligatorio.") @Pattern(regexp = "(?i)[MF]", message = "El género debe ser M o F.") String genero,
        @Max(value = 1, message = "El estado debe ser 0 o 1.") @Pattern(regexp = "[01]", message = "El estado debe ser 0 o 1.") String estado,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no tiene un formato válido.") @Size(max = 100, message = "El correo no puede superar 100 caracteres.") String correo,
        @NotBlank(message = "El teléfono es obligatorio.") @Size(max = 20, message = "El teléfono no puede superar 20 caracteres.") String telefono,
        @NotNull(message = "El tipo de persona es obligatorio.") @Pattern(regexp = "(?i)[AI]", message = "El tipo de persona debe ser A o I.") String tipoPersona,
        @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "La foto externa debe ser una URL HTTP o HTTPS válida.")
        @Size(max = 255, message = "La foto no puede superar 255 caracteres.") String foto) {
}
