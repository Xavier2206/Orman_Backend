package com.orman.backend.auth.dto.request;

import com.orman.backend.auth.model.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El login es obligatorio.")
        @Size(max = 30, message = "El login no puede superar 30 caracteres.")
        String login,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String password,
        @NotBlank(message = "El deviceId es obligatorio.")
        @Size(max = 100, message = "El deviceId no puede superar 100 caracteres.")
        String deviceId,
        @NotBlank(message = "El deviceName es obligatorio.")
        @Size(max = 100, message = "El deviceName no puede superar 100 caracteres.")
        String deviceName,
        @NotNull(message = "El clientType es obligatorio.")
        ClientType clientType) {

    @Override
    public String toString() {
        return "LoginRequest[login=" + login + ", password=<redacted>, deviceId=" + deviceId
                + ", deviceName=" + deviceName + ", clientType=" + clientType + "]";
    }
}
