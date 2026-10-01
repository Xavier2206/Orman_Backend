package com.orman.backend.notification.dto.response;

import java.time.OffsetDateTime;

public record NotificacionResponse(Long codnot, String tipo, String titulo, String mensaje,
                                   String referenciaTipo, Integer referenciaId,
                                   OffsetDateTime fechaCreacion, boolean leida,
                                   OffsetDateTime fechaLectura) {
}
