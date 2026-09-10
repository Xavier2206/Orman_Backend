package com.orman.backend.notification.dto.response;

import java.time.LocalDateTime;

public record NotificacionResponse(Long codnot, String tipo, String titulo, String mensaje,
                                   String referenciaTipo, Integer referenciaId,
                                   LocalDateTime fechaCreacion, boolean leida,
                                   LocalDateTime fechaLectura) {
}
