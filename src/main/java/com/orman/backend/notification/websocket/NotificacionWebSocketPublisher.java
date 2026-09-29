package com.orman.backend.notification.websocket;

import com.orman.backend.notification.event.NotificacionDisponibleEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificacionWebSocketPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionWebSocketPublisher.class);
    private static final String PRIVATE_DESTINATION = "/queue/notificaciones";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificacionDisponibleEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(event.loginDestinatario(), PRIVATE_DESTINATION,
                    new NotificacionDisponiblePayload(event.codnot(), event.tipo()));
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudo entregar mensaje WebSocket: cause={}",
                    exception.getClass().getSimpleName());
        }
    }
}
