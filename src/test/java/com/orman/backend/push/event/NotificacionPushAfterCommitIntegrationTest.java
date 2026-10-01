package com.orman.backend.push.event;

import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.event.NotificacionDisponibleEvent;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "orman.firebase.enabled=true")
class NotificacionPushAfterCommitIntegrationTest {

    private static final NotificacionDisponibleEvent EVENT =
            new NotificacionDisponibleEvent(810L, "tenant.after.commit", NotificacionTipo.CUOTA_VENCIDA);
    private static final PushNotificationMessage MESSAGE = new PushNotificationMessage(810L,
            "tenant.after.commit", NotificacionTipo.CUOTA_VENCIDA, "ORMAN", "Tienes una cuota vencida.",
            Map.of("tipo", "CUOTA_VENCIDA", "codnot", "810", "referenciaTipo", "CUOTA",
                    "referenciaId", "91", "codcuo", "91"));
    private static final List<PushDestination> DESTINATIONS = List.of(new PushDestination(42L, "fid-after-commit"));

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;
    @MockitoBean private PushNotificationResolver notificationResolver;
    @MockitoBean private PushDestinationService destinationService;
    @MockitoBean private PushGateway pushGateway;

    @BeforeEach
    void configurePushMocks() {
        when(pushGateway.isAvailable()).thenReturn(true);
        when(notificationResolver.find(EVENT.codnot())).thenReturn(Optional.of(MESSAGE));
        when(destinationService.findEligible(EVENT.loginDestinatario())).thenReturn(DESTINATIONS);
        when(pushGateway.send(DESTINATIONS, MESSAGE)).thenReturn(
                List.of(new PushSendResult(42L, PushSendStatus.SENT)));
    }

    @Test
    void publishesPushAfterTheTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(EVENT));

        verify(pushGateway).send(DESTINATIONS, MESSAGE);
    }

    @Test
    void doesNotPublishPushWhenTheTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(EVENT);
            status.setRollbackOnly();
        });

        verifyNoInteractions(notificationResolver, destinationService, pushGateway);
    }

    @Test
    void fcmFailureAfterCommitDoesNotFailTheCommittedTransaction() {
        when(pushGateway.send(DESTINATIONS, MESSAGE)).thenThrow(new IllegalStateException("temporary FCM outage"));

        assertThatCode(() -> transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(EVENT)))
                .doesNotThrowAnyException();

        verify(pushGateway).send(DESTINATIONS, MESSAGE);
    }
}
