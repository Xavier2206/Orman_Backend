package com.orman.backend.notification.service.impl;

import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.payment.event.PagoConfirmadoEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class PagoNotificacionEventHandlerTest {

    @Mock private NotificacionGeneracionService notificacionGeneracionService;

    @Test
    void handlesFinancialNotificationAfterCommitAndContainsNotificationFailures() throws Exception {
        PagoNotificacionEventHandler handler = new PagoNotificacionEventHandler(notificacionGeneracionService);
        doThrow(new IllegalStateException("notification unavailable"))
                .when(notificacionGeneracionService).generatePaymentConfirmed(42);

        assertThatCode(() -> handler.handle(new PagoConfirmadoEvent(42))).doesNotThrowAnyException();

        Method method = PagoNotificacionEventHandler.class.getMethod("handle", PagoConfirmadoEvent.class);
        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
