package com.orman.backend.notification.websocket;

import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacionOutboundAuthorizationInterceptorTest {

    private static final String LOGIN = "tenant.socket";
    private static final String SESSION_ID = "socket-session";
    private static final UUID SID = UUID.fromString("22000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");

    private SessionService sessionService;
    private JwtService jwtService;
    private NotificacionStompChannelInterceptor inbound;
    private NotificacionOutboundAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        UserAuthorityService userAuthorityService = mock(UserAuthorityService.class);
        jwtService = mock(JwtService.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        inbound = new NotificacionStompChannelInterceptor(jwtService, sessionService, userAuthorityService, clock);
        interceptor = new NotificacionOutboundAuthorizationInterceptor(inbound, clock);
        when(jwtService.validateAndExtract("valid-token"))
                .thenReturn(new JwtService.JwtClaims(LOGIN, SID, "orman-backend", NOW.minusSeconds(30),
                        NOW.plusSeconds(60)));
        when(sessionService.authenticate(LOGIN, SID)).thenReturn(new AuthenticatedUser(LOGIN, SID));
        when(userAuthorityService.loadAuthorities(LOGIN))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_INQUILINO")));
        inbound.preSend(connect("valid-token", SESSION_ID), null);
    }

    @Test
    void allowsResolvedPrivateBrokerMessageWhenPrincipalIsAbsent() {
        Message<?> message = outbound(null, SESSION_ID, "/queue/notificaciones-user" + SESSION_ID, "sub-1");

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void allowsResolvedPrivateBrokerMessageWhenPrincipalMatchesConnectSession() {
        WebSocketUserPrincipal principal = new WebSocketUserPrincipal(LOGIN, SID, NOW.plusSeconds(60));
        Message<?> message = outbound(principal, SESSION_ID, "/queue/notificaciones-user" + SESSION_ID, "sub-1");

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void rejectsBroadcastUnresolvedAndMismatchedSessionMessages() {
        assertThat(interceptor.preSend(
                outbound(null, SESSION_ID, "/topic/notificaciones", "sub-1"), null)).isNull();
        assertThat(interceptor.preSend(
                outbound(null, SESSION_ID, "/queue/notificaciones-user-other", "sub-1"), null)).isNull();
        assertThat(interceptor.preSend(
                outbound(null, "unknown-session", "/queue/notificaciones-userunknown-session", "sub-1"), null))
                .isNull();
        assertThat(interceptor.preSend(
                outbound(null, SESSION_ID, "/queue/notificaciones-user" + SESSION_ID, null), null)).isNull();
        assertThat(interceptor.preSend(outbound(
                new WebSocketUserPrincipal("other.user", UUID.randomUUID(), NOW.plusSeconds(60)), SESSION_ID,
                "/queue/notificaciones-user" + SESSION_ID, "sub-1"), null)).isNull();
    }

    @Test
    void dropsDeliveryToRevokedSessionAndExpiredAccessToken() {
        when(sessionService.authenticate(LOGIN, SID)).thenThrow(new RevokedSessionException());
        assertThat(interceptor.preSend(
                outbound(null, SESSION_ID, "/queue/notificaciones-user" + SESSION_ID, "sub-1"), null)).isNull();

        UUID expiredSid = UUID.fromString("22000000-0000-0000-0000-000000000003");
        String expiredSession = "expired-session";
        when(jwtService.validateAndExtract("expired-token"))
                .thenReturn(new JwtService.JwtClaims(LOGIN, expiredSid, "orman-backend", NOW.minusSeconds(90),
                        NOW.minusSeconds(1)));
        when(sessionService.authenticate(LOGIN, expiredSid)).thenReturn(new AuthenticatedUser(LOGIN, expiredSid));
        inbound.preSend(connect("expired-token", expiredSession), null);
        assertThat(interceptor.preSend(outbound(null, expiredSession,
                "/queue/notificaciones-user" + expiredSession, "sub-2"), null)).isNull();
    }

    private Message<?> connect(String token, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> outbound(WebSocketUserPrincipal principal, String sessionId, String destination,
                                String subscriptionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        if (principal != null) {
            accessor.setUser(principal);
        }
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage("{\"codnot\":41}", accessor.getMessageHeaders());
    }
}
