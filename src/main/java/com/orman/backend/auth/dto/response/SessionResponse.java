package com.orman.backend.auth.dto.response;

import com.orman.backend.auth.model.ClientType;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID sid,
        String deviceId,
        String deviceName,
        ClientType clientType,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        LocalDateTime ultimoUso,
        boolean current) {
}
