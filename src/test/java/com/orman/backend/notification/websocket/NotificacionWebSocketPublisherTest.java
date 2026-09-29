package com.orman.backend.notification.websocket;

import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.event.NotificacionDisponibleEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificacionWebSocketPublisherTest {

    @Test
    void sendsSmallPayloadOnlyToTheNotificationRecipient() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        NotificacionWebSocketPublisher publisher = new NotificacionWebSocketPublisher(template);

        publisher.publish(new NotificacionDisponibleEvent(742L, "tenant.socket", NotificacionTipo.PAGO_CONFIRMADO));

        var payload = org.mockito.ArgumentCaptor.forClass(NotificacionDisponiblePayload.class);
        verify(template).convertAndSendToUser(org.mockito.ArgumentMatchers.eq("tenant.socket"),
                org.mockito.ArgumentMatchers.eq("/queue/notificaciones"), payload.capture());
        assertThat(payload.getValue()).isEqualTo(
                new NotificacionDisponiblePayload(742L, NotificacionTipo.PAGO_CONFIRMADO));
    }

    @Test
    void containsTransportFailureAndListenerRunsAfterCommit() throws Exception {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(template).convertAndSendToUser("tenant.socket", "/queue/notificaciones",
                        new NotificacionDisponiblePayload(743L, NotificacionTipo.PAGO_RECHAZADO));
        NotificacionWebSocketPublisher publisher = new NotificacionWebSocketPublisher(template);

        publisher.publish(new NotificacionDisponibleEvent(743L, "tenant.socket", NotificacionTipo.PAGO_RECHAZADO));

        verify(template).convertAndSendToUser("tenant.socket", "/queue/notificaciones",
                new NotificacionDisponiblePayload(743L, NotificacionTipo.PAGO_RECHAZADO));
        TransactionalEventListener listener = NotificacionWebSocketPublisher.class
                .getMethod("publish", NotificacionDisponibleEvent.class)
                .getAnnotation(TransactionalEventListener.class);
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
