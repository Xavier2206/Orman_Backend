package com.orman.backend.push.service.impl;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.SendResponse;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.push.config.FirebaseProperties;
import com.orman.backend.push.service.PushDestination;
import com.orman.backend.push.service.PushNotificationMessage;
import com.orman.backend.push.service.PushSendStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirebasePushGatewayTest {

    @Test
    void missingCredentialsDoNotBreakDeliveryOrRetryFirebaseInitializationForEveryNotification() {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(true);
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenThrow(new BeanCreationException("firebaseMessaging",
                "Application Default Credentials unavailable"));
        FirebasePushGateway gateway = new FirebasePushGateway(properties, provider);

        assertThat(gateway.isAvailable()).isFalse();
        assertThat(gateway.isAvailable()).isFalse();
        verify(provider).getIfAvailable();
    }

    @Test
    void buildsMessagesWithFidsAndCorrelatesEachResult() throws Exception {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(true);
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messaging);

        SendResponse sent = mock(SendResponse.class);
        when(sent.isSuccessful()).thenReturn(true);
        SendResponse invalid = mock(SendResponse.class);
        FirebaseMessagingException unregistered = mock(FirebaseMessagingException.class);
        when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(invalid.isSuccessful()).thenReturn(false);
        when(invalid.getException()).thenReturn(unregistered);
        BatchResponse response = mock(BatchResponse.class);
        when(response.getResponses()).thenReturn(List.of(sent, invalid));
        when(messaging.sendEach(anyList())).thenReturn(response);

        FirebasePushGateway gateway = new FirebasePushGateway(properties, provider);
        List<PushDestination> destinations = List.of(new PushDestination(12L, "fid-one"),
                new PushDestination(13L, "fid-two"));
        List<?> results = gateway.send(destinations, payload());

        var messagesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(messaging).sendEach(messagesCaptor.capture());
        List<Message> messages = (List<Message>) messagesCaptor.getValue();
        assertThat(messages).hasSize(2);
        assertThat(ReflectionTestUtils.getField(messages.get(0), "fid")).isEqualTo("fid-one");
        assertThat(ReflectionTestUtils.getField(messages.get(0), "token")).isNull();
        assertThat(ReflectionTestUtils.getField(messages.get(1), "fid")).isEqualTo("fid-two");
        assertThat(results).extracting("status").containsExactly(
                PushSendStatus.SENT, PushSendStatus.INVALID_DESTINATION);
    }

    @Test
    void invalidArgumentIsNotClassifiedAsAnInvalidInstallation() throws Exception {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setEnabled(true);
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messaging);
        FirebaseMessagingException invalidArgument = mock(FirebaseMessagingException.class);
        when(invalidArgument.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        SendResponse failed = mock(SendResponse.class);
        when(failed.isSuccessful()).thenReturn(false);
        when(failed.getException()).thenReturn(invalidArgument);
        BatchResponse response = mock(BatchResponse.class);
        when(response.getResponses()).thenReturn(List.of(failed));
        when(messaging.sendEach(anyList())).thenReturn(response);

        FirebasePushGateway gateway = new FirebasePushGateway(properties, provider);

        assertThat(gateway.send(List.of(new PushDestination(15L, "fid-still-valid")), payload()))
                .extracting("status").containsExactly(PushSendStatus.FAILED);
    }

    private PushNotificationMessage payload() {
        return new PushNotificationMessage(700L, "tenant.mobile", NotificacionTipo.PAGO_CONFIRMADO,
                "ORMAN", "Tu pago fue confirmado. Toca para consultar la cuota.",
                Map.of("tipo", "PAGO_CONFIRMADO", "codnot", "700", "referenciaTipo", "PAGO",
                        "referenciaId", "8", "codcuo", "9"));
    }
}
