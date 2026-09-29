package com.orman.backend.notification.websocket;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

public record WebSocketUserPrincipal(String login, UUID sid, Instant accessTokenExpiresAt) implements Principal {

    @Override
    public String getName() {
        return login;
    }
}
