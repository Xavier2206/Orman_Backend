package com.orman.backend.notification.event;

import com.orman.backend.notification.entity.NotificacionTipo;
import java.util.Objects;

/** Internal signal published in the transaction that creates or reopens a notification. */
public record NotificacionDisponibleEvent(Long codnot, String loginDestinatario, NotificacionTipo tipo) {

    public NotificacionDisponibleEvent {
        Objects.requireNonNull(codnot);
        Objects.requireNonNull(loginDestinatario);
        Objects.requireNonNull(tipo);
    }
}
