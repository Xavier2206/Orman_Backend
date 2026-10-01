package com.orman.backend.push.event;

import com.orman.backend.notification.event.NotificacionDisponibleEvent;
import com.orman.backend.push.config.FirebaseProperties;
import com.orman.backend.push.service.PushDestination;
import com.orman.backend.push.service.PushGateway;
import com.orman.backend.push.service.PushNotificationMessage;
import com.orman.backend.push.service.PushSendResult;
import com.orman.backend.push.service.PushSendStatus;
import com.orman.backend.push.service.impl.PushDestinationService;
import com.orman.backend.push.service.impl.PushNotificationResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificacionPushListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionPushListener.class);

    private final FirebaseProperties properties;
    private final PushNotificationResolver notificationResolver;
    private final PushDestinationService destinationService;
    private final PushGateway pushGateway;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificacionDisponibleEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            if (!pushGateway.isAvailable()) {
                return;
            }
            notificationResolver.find(event.codnot()).ifPresent(notification -> deliver(event, notification));
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo entregar por FCM la notificación {} tipo {}: cause={}", event.codnot(),
                    event.tipo(), exception.getClass().getSimpleName());
        }
    }

    private void deliver(NotificacionDisponibleEvent event, PushNotificationMessage notification) {
        if (!event.loginDestinatario().equals(notification.loginDestinatario())
                || event.tipo() != notification.tipo()) {
            return;
        }

        List<PushDestination> destinations = destinationService.findEligible(notification.loginDestinatario());
        if (destinations.isEmpty()) {
            return;
        }

        List<PushSendResult> results = pushGateway.send(destinations, notification);
        int sent = 0;
        int failed = 0;
        for (com.orman.backend.push.service.PushSendResult result : results) {
            if (result.status() == PushSendStatus.SENT) {
                sent++;
            } else {
                failed++;
            }
            if (result.status() == PushSendStatus.INVALID_DESTINATION) {
                deactivateInvalidDestination(result.coddis(), event.codnot());
            }
        }
        if (failed > 0) {
            LOGGER.warn("Entrega FCM de notificación {} tipo {}: destinos={}, enviados={}, fallidos={}",
                    event.codnot(), event.tipo(), destinations.size(), sent, failed);
        } else {
            LOGGER.info("Entrega FCM de notificación {} tipo {}: destinos={}, enviados={}, fallidos={}",
                    event.codnot(), event.tipo(), destinations.size(), sent, failed);
        }
    }

    private void deactivateInvalidDestination(Long coddis, Long codnot) {
        try {
            destinationService.deactivate(coddis);
        } catch (RuntimeException exception) {
            LOGGER.warn("No se pudo desactivar el destino push {} tras notificación {}: cause={}", coddis,
                    codnot, exception.getClass().getSimpleName());
        }
    }
}
