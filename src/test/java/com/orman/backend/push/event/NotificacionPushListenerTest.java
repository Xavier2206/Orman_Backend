package com.orman.backend.push.event;

import com.orman.backend.notification.entity.NotificacionTipo;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificacionPushListenerTest {

    private static final NotificacionDisponibleEvent EVENT =
            new NotificacionDisponibleEvent(123L, "tenant.mobile", NotificacionTipo.PAGO_CONFIRMADO);

    private FirebaseProperties properties;
    private PushNotificationResolver resolver;
    private PushDestinationService destinationService;
    private PushGateway gateway;
    private NotificacionPushListener listener;

    @BeforeEach
    void setUp() {
        properties = new FirebaseProperties();
        resolver = mock(PushNotificationResolver.class);
        destinationService = mock(PushDestinationService.class);
        gateway = mock(PushGateway.class);
        listener = new NotificacionPushListener(properties, resolver, destinationService, gateway);
    }

    @Test
    void disabledFirebaseDoesNotResolveOrSend() {
        listener.publish(EVENT);

        verifyNoInteractions(resolver, destinationService, gateway);
    }

    @Test
    void unsupportedNotificationOrNoDevicesDoesNothing() {
        properties.setEnabled(true);
        when(gateway.isAvailable()).thenReturn(true);
        when(resolver.find(123L)).thenReturn(Optional.empty());

        listener.publish(EVENT);

        verifyNoInteractions(destinationService);
        verify(gateway, never()).send(anyList(), org.mockito.ArgumentMatchers.any());

        when(resolver.find(123L)).thenReturn(Optional.of(notification()));
        when(destinationService.findEligible("tenant.mobile")).thenReturn(List.of());
        listener.publish(EVENT);
        verify(gateway, never()).send(anyList(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendsToEveryEligibleDeviceAndDeactivatesOnlyUnregisteredFid() {
        properties.setEnabled(true);
        when(gateway.isAvailable()).thenReturn(true);
        PushNotificationMessage notification = notification();
        when(resolver.find(123L)).thenReturn(Optional.of(notification));
        List<PushDestination> destinations = List.of(new PushDestination(5L, "fid-a"),
                new PushDestination(6L, "fid-b"));
        when(destinationService.findEligible("tenant.mobile")).thenReturn(destinations);
        when(gateway.send(destinations, notification)).thenReturn(List.of(
                new PushSendResult(5L, PushSendStatus.SENT),
                new PushSendResult(6L, PushSendStatus.INVALID_DESTINATION)));

        listener.publish(EVENT);

        verify(gateway).send(eq(destinations), eq(notification));
        verify(destinationService).deactivate(6L);
        verify(destinationService, never()).deactivate(5L);
    }

    @Test
    void temporaryFailureDoesNotDeactivateDestinationAndFcmExceptionsStayIsolated() {
        properties.setEnabled(true);
        when(gateway.isAvailable()).thenReturn(true);
        PushNotificationMessage notification = notification();
        when(resolver.find(123L)).thenReturn(Optional.of(notification));
        List<PushDestination> destinations = List.of(new PushDestination(5L, "fid-a"));
        when(destinationService.findEligible("tenant.mobile")).thenReturn(destinations);
        when(gateway.send(destinations, notification)).thenReturn(
                List.of(new PushSendResult(5L, PushSendStatus.FAILED)));

        listener.publish(EVENT);
        verify(destinationService, never()).deactivate(5L);

        when(gateway.send(destinations, notification)).thenThrow(new IllegalStateException("network timeout"));
        org.assertj.core.api.Assertions.assertThatCode(() -> listener.publish(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void listenerIsRegisteredAfterCommit() throws Exception {
        TransactionalEventListener annotation = NotificacionPushListener.class
                .getMethod("publish", NotificacionDisponibleEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    private PushNotificationMessage notification() {
        return new PushNotificationMessage(123L, "tenant.mobile", NotificacionTipo.PAGO_CONFIRMADO,
                "ORMAN", "Tu pago fue confirmado. Toca para consultar la cuota.",
                Map.of("tipo", "PAGO_CONFIRMADO", "codnot", "123", "referenciaTipo", "PAGO",
                        "referenciaId", "75", "codcuo", "80"));
    }
}
