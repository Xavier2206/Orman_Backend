package com.orman.backend.auth.dto.response;

import com.orman.backend.auth.model.ClientType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID sid,
        String deviceId,
        String deviceName,
        ClientType clientType,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaExpiracion,
        OffsetDateTime ultimoUso,
        boolean current) {
}
