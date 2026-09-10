package com.orman.backend.notification.service.impl;

import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.payment.event.ComprobanteRecibidoEvent;
import com.orman.backend.payment.event.PagoConfirmadoEvent;
import com.orman.backend.payment.event.PagoRechazadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagoNotificacionEventHandler {

    private final NotificacionGeneracionService notificacionGeneracionService;

    @EventListener
    public void handle(PagoConfirmadoEvent event) {
        notificacionGeneracionService.generatePaymentConfirmed(event.codpag());
    }

    @EventListener
    public void handle(PagoRechazadoEvent event) {
        notificacionGeneracionService.generatePaymentRejected(event.codpag());
    }

    @EventListener
    public void handle(ComprobanteRecibidoEvent event) {
        notificacionGeneracionService.generateComprobanteReceived(event.codpag());
    }
}
