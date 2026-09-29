package com.orman.backend.notification.websocket;

import com.orman.backend.auth.exception.ExpiredJwtException;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacionStompChannelInterceptorTest {

    private static final String LOGIN = "tenant.socket";
    private static final UUID SID = UUID.fromString("11000000-0000-0000-0000-000000000001");

    private JwtService jwtService;
    private SessionService sessionService;
    private UserAuthorityService userAuthorityService;
    private NotificacionStompChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        sessionService = mock(SessionService.class);
        userAuthorityService = mock(UserAuthorityService.class);
        interceptor = new NotificacionStompChannelInterceptor(jwtService, sessionService, userAuthorityService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        when(jwtService.validateAndExtract("valid-token"))
                .thenReturn(new JwtService.JwtClaims(LOGIN, SID, "orman-backend", Instant.EPOCH,
                        Instant.parse("2030-01-01T00:00:00Z")));
        when(sessionService.authenticate(LOGIN, SID)).thenReturn(new AuthenticatedUser(LOGIN, SID));
        when(userAuthorityService.loadAuthorities(LOGIN))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_INQUILINO")));
    }

    @Test
    void acceptsConnectWithJwtAndSetsLoginPrincipal() {
        Message<?> result = interceptor.preSend(connect("Bearer valid-token"), null);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor.getUser()).isInstanceOf(WebSocketUserPrincipal.class);
        WebSocketUserPrincipal principal = (WebSocketUserPrincipal) accessor.getUser();
        assertThat(principal.getName()).isEqualTo(LOGIN);
        assertThat(principal.sid()).isEqualTo(SID);
    }

    @Test
    void rejectsConnectWithoutAuthorization() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void rejectsMalformedOrInvalidBearerToken() {
        when(jwtService.validateAndExtract("wrong-token")).thenThrow(new InvalidJwtException());

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer  wrong-token"), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer wrong-token"), null))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rejectsExpiredJwt() {
        when(jwtService.validateAndExtract("expired-token")).thenThrow(new ExpiredJwtException());

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer expired-token"), null))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsRevokedSessionAndInactiveAccount() {
        when(sessionService.authenticate(LOGIN, SID)).thenThrow(new RevokedSessionException());
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer valid-token"), null))
                .isInstanceOf(RevokedSessionException.class);

        org.mockito.Mockito.doThrow(new InvalidJwtException()).when(sessionService).authenticate(LOGIN, SID);
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer valid-token"), null))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void onlyAllowsAuthenticatedUserToSubscribeToItsPrivateNotificationDestination() {
        WebSocketUserPrincipal principal = principal();
        Message<?> result = interceptor.preSend(subscribe(principal, "/user/queue/notificaciones"), null);
        assertThat(result).isNotNull();

        assertThatThrownBy(() -> interceptor.preSend(subscribe(principal,
                "/user/another.login/queue/notificaciones"), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(subscribe(principal, "/topic/notificaciones"), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void rejectsClientPublishFramesAndUnattachedSubscriptions() {
        StompHeaderAccessor send = StompHeaderAccessor.create(StompCommand.SEND);
        send.setDestination("/app/internal");
        send.setLeaveMutable(true);
        Message<?> sendMessage = MessageBuilder.createMessage(new byte[0], send.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(sendMessage, null))
                .isInstanceOf(MessageDeliveryException.class);

        assertThatThrownBy(() -> interceptor.preSend(subscribe(null, "/user/queue/notificaciones"), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void acceptsInternalDisconnectWithoutPrincipalAndClearsAuthenticatedSession() {
        String sessionId = "disconnect-session";
        Message<?> connected = connect("Bearer valid-token", sessionId);
        interceptor.preSend(connected, null);
        assertThat(interceptor.principalForSession(sessionId)).isNotNull();

        StompHeaderAccessor disconnect = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        disconnect.setSessionId(sessionId);
        disconnect.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], disconnect.getMessageHeaders());

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
        assertThat(interceptor.principalForSession(sessionId)).isNull();
    }

    @Test
    void rejectsAccountWithoutNotificationRole() {
        when(userAuthorityService.loadAuthorities(LOGIN)).thenReturn(List.of());

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer valid-token"), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private Message<?> connect(String authorization) {
        return connect(authorization, "socket-session");
    }

    private Message<?> connect(String authorization, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(sessionId);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> subscribe(WebSocketUserPrincipal principal, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(principal);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private WebSocketUserPrincipal principal() {
        return new WebSocketUserPrincipal(LOGIN, SID, Instant.parse("2030-01-01T00:00:00Z"));
    }
}
