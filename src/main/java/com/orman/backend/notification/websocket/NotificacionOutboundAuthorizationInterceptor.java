package com.orman.backend.notification.websocket;

import java.security.Principal;
import java.time.Clock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

public class NotificacionOutboundAuthorizationInterceptor implements ChannelInterceptor {

    private static final String RESOLVED_PRIVATE_DESTINATION_PREFIX = "/queue/notificaciones-user";

    private final NotificacionStompChannelInterceptor authorization;
    private final Clock clock;

    public NotificacionOutboundAuthorizationInterceptor(NotificacionStompChannelInterceptor authorization, Clock clock) {
        this.authorization = authorization;
        this.clock = clock;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
        if (accessor == null || accessor.getMessageType() != SimpMessageType.MESSAGE) {
            return message;
        }

        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        String destination = accessor.getDestination();
        if (sessionId == null || subscriptionId == null
                || !(RESOLVED_PRIVATE_DESTINATION_PREFIX + sessionId).equals(destination)) {
            return null;
        }

        Principal user = accessor.getUser();
        if (user != null && !(user instanceof WebSocketUserPrincipal)) {
            return null;
        }
        WebSocketUserPrincipal principal = authorization.principalForSession(sessionId);
        if (principal == null || user instanceof WebSocketUserPrincipal messagePrincipal
                && !principal.equals(messagePrincipal)) {
            return null;
        }
        if (!principal.accessTokenExpiresAt().isAfter(clock.instant())) {
            return null;
        }
        try {
            authorization.authorizeCurrentSession(principal);
            return message;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
