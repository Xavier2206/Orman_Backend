package com.orman.backend.push.service;

import java.util.List;

public interface PushGateway {

    boolean isAvailable();

    List<PushSendResult> send(List<PushDestination> destinations, PushNotificationMessage notification);
}
