package com.orman.backend.notification.websocket;

import com.orman.backend.notification.entity.NotificacionTipo;

public record NotificacionDisponiblePayload(Long codnot, NotificacionTipo tipo) {
}
