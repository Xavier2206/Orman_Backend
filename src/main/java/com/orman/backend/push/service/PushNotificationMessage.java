package com.orman.backend.push.service;

import com.orman.backend.notification.entity.NotificacionTipo;
import java.util.Map;

public record PushNotificationMessage(Long codnot, String loginDestinatario, NotificacionTipo tipo,
                                      String title, String body, Map<String, String> data) {

    public PushNotificationMessage {
        data = Map.copyOf(data);
    }
}
