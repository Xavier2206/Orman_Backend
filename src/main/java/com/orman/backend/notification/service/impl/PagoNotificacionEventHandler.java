package com.orman.backend.notification.service.impl;

import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.payment.event.ComprobanteRecibidoEvent;
import com.orman.backend.payment.event.PagoConfirmadoEvent;
import com.orman.backend.payment.event.PagoRechazadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PagoNotificacionEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagoNotificacionEventHandler.class);

    private final NotificacionGeneracionService notificacionGeneracionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PagoConfirmadoEvent event) {
        safely("confirmación", event.codpag(), () -> notificacionGeneracionService.generatePaymentConfirmed(event.codpag()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PagoRechazadoEvent event) {
        safely("rechazo", event.codpag(), () -> notificacionGeneracionService.generatePaymentRejected(event.codpag()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ComprobanteRecibidoEvent event) {
        safely("comprobante", event.codpag(), () -> notificacionGeneracionService.generateComprobanteReceived(event.codpag()));
    }

    private void safely(String action, Integer codpag, Runnable notificationAction) {
        try {
            notificationAction.run();
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudo generar la notificación after-commit para pago {} y evento {}: {}",
                    codpag, action, exception.getClass().getSimpleName());
        }
    }
}
