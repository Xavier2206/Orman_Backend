package com.orman.backend.push.service.impl;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.orman.backend.push.config.FirebaseProperties;
import com.orman.backend.push.service.PushDestination;
import com.orman.backend.push.service.PushGateway;
import com.orman.backend.push.service.PushNotificationMessage;
import com.orman.backend.push.service.PushSendResult;
import com.orman.backend.push.service.PushSendStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebasePushGateway implements PushGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebasePushGateway.class);
    private static final int MAX_BATCH_SIZE = 500;

    private final FirebaseProperties properties;
    private final ObjectProvider<FirebaseMessaging> messagingProvider;
    private volatile FirebaseMessaging messaging;
    private volatile boolean initializationFailed;

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && getMessaging() != null;
    }

    @Override
    public List<PushSendResult> send(List<PushDestination> destinations, PushNotificationMessage notification) {
        FirebaseMessaging firebaseMessaging = getMessaging();
        if (firebaseMessaging == null) {
            return destinations.stream()
                    .map(destination -> new PushSendResult(destination.coddis(), PushSendStatus.FAILED))
                    .toList();
        }

        List<PushSendResult> results = new ArrayList<>(destinations.size());
        for (int start = 0; start < destinations.size(); start += MAX_BATCH_SIZE) {
            List<PushDestination> batch = destinations.subList(start,
                    Math.min(start + MAX_BATCH_SIZE, destinations.size()));
            sendBatch(firebaseMessaging, batch, notification, results);
        }
        return List.copyOf(results);
    }

    private void sendBatch(FirebaseMessaging firebaseMessaging, List<PushDestination> destinations,
                           PushNotificationMessage notification, List<PushSendResult> results) {
        List<Message> messages = destinations.stream()
                .map(destination -> Message.builder()
                        .setFid(destination.installationId())
                        .setNotification(Notification.builder()
                                .setTitle(notification.title())
                                .setBody(notification.body())
                                .build())
                        .putAllData(notification.data())
                        .build())
                .toList();
        try {
            BatchResponse response = firebaseMessaging.sendEach(messages);
            List<SendResponse> responses = response.getResponses();
            for (int index = 0; index < destinations.size(); index++) {
                PushDestination destination = destinations.get(index);
                SendResponse sendResponse = responses.get(index);
                PushSendStatus status = classify(sendResponse);
                results.add(new PushSendResult(destination.coddis(), status));
            }
        } catch (FirebaseMessagingException | RuntimeException exception) {
            for (PushDestination destination : destinations) {
                results.add(new PushSendResult(destination.coddis(), PushSendStatus.FAILED));
            }
        }
    }

    private PushSendStatus classify(SendResponse response) {
        if (response.isSuccessful()) {
            return PushSendStatus.SENT;
        }
        if (response.getException() instanceof FirebaseMessagingException exception
                && exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            return PushSendStatus.INVALID_DESTINATION;
        }
        return PushSendStatus.FAILED;
    }

    private FirebaseMessaging getMessaging() {
        if (!properties.isEnabled() || initializationFailed) {
            return null;
        }
        FirebaseMessaging current = messaging;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (messaging != null || initializationFailed) {
                return messaging;
            }
            try {
                messaging = messagingProvider.getIfAvailable();
                if (messaging == null) {
                    initializationFailed = true;
                    LOGGER.warn("Firebase Admin no está disponible; se omitirán los envíos push.");
                }
            } catch (BeansException | IllegalStateException exception) {
                initializationFailed = true;
                LOGGER.warn("Firebase Admin no está configurado; se omitirán los envíos push.");
            }
            return messaging;
        }
    }
}
