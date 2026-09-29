package com.orman.backend.notification.websocket;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import java.security.Principal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

public class NotificacionStompChannelInterceptor implements ChannelInterceptor {

    static final String PRIVATE_NOTIFICATION_SUBSCRIPTION = "/user/queue/notificaciones";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String OWNER_ROLE = "ROLE_PROPIETARIO";
    private static final String TENANT_ROLE = "ROLE_INQUILINO";

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final UserAuthorityService userAuthorityService;
    private final Clock clock;
    private final ConcurrentMap<String, WebSocketUserPrincipal> principalsBySession = new ConcurrentHashMap<>();

    public NotificacionStompChannelInterceptor(JwtService jwtService, SessionService sessionService,
                                                UserAuthorityService userAuthorityService, Clock clock) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.userAuthorityService = userAuthorityService;
        this.clock = clock;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            throw new MessageDeliveryException("Trama STOMP no autorizada.");
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            WebSocketUserPrincipal principal = authenticate(accessor);
            accessor.setUser(principal);
            if (accessor.getSessionId() != null) {
                principalsBySession.put(accessor.getSessionId(), principal);
            }
            return message;
        }
        if (command == StompCommand.SUBSCRIBE) {
            WebSocketUserPrincipal principal = requirePrincipal(accessor.getUser());
            authorizeCurrentSession(principal);
            if (!PRIVATE_NOTIFICATION_SUBSCRIPTION.equals(accessor.getDestination())) {
                throw new MessageDeliveryException("Destino de suscripción no autorizado.");
            }
            return message;
        }
        if (command == StompCommand.DISCONNECT) {
            // Spring can synthesize this teardown frame after the socket closes without restoring its Principal.
            // DISCONNECT only releases transport state; CONNECT, SUBSCRIBE and SEND keep their authorization checks.
            Principal user = accessor.getUser();
            WebSocketUserPrincipal registered = principalForSession(accessor.getSessionId());
            if (user != null && !(user instanceof WebSocketUserPrincipal)) {
                throw new MessageDeliveryException("Conexión STOMP no autenticada.");
            }
            if (user instanceof WebSocketUserPrincipal principal && registered != null
                    && !registered.equals(principal)) {
                throw new MessageDeliveryException("Conexión STOMP no autenticada.");
            }
            removeSessionPrincipal(accessor.getSessionId());
            return message;
        }
        if (command == StompCommand.UNSUBSCRIBE) {
            requirePrincipal(accessor.getUser());
            return message;
        }
        throw new MessageDeliveryException("Operación STOMP no autorizada.");
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        removeSessionPrincipal(event.getSessionId());
    }

    WebSocketUserPrincipal principalForSession(String sessionId) {
        return sessionId == null ? null : principalsBySession.get(sessionId);
    }

    private void removeSessionPrincipal(String sessionId) {
        if (sessionId != null) {
            principalsBySession.remove(sessionId);
        }
    }

    private WebSocketUserPrincipal authenticate(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader(HttpHeaders.AUTHORIZATION);
        if (headers == null || headers.size() != 1) {
            throw new MessageDeliveryException("Se requiere Bearer válido en STOMP CONNECT.");
        }
        String authorization = headers.getFirst();
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()
                || !authorization.substring(BEARER_PREFIX.length())
                        .equals(authorization.substring(BEARER_PREFIX.length()).trim())) {
            throw new MessageDeliveryException("Se requiere Bearer válido en STOMP CONNECT.");
        }

        JwtService.JwtClaims claims = jwtService.validateAndExtract(authorization.substring(BEARER_PREFIX.length()));
        AuthenticatedUser authenticated = sessionService.authenticate(claims.subject(), claims.sid());
        WebSocketUserPrincipal principal = new WebSocketUserPrincipal(authenticated.login(), authenticated.sid(),
                claims.expiresAt());
        authorizeNotificationRole(principal.login());
        return principal;
    }

    static WebSocketUserPrincipal requirePrincipal(Principal principal) {
        if (principal instanceof WebSocketUserPrincipal authenticated) {
            return authenticated;
        }
        throw new MessageDeliveryException("Conexión STOMP no autenticada.");
    }

    void authorizeCurrentSession(WebSocketUserPrincipal principal) {
        if (!principal.accessTokenExpiresAt().isAfter(clock.instant())) {
            throw new MessageDeliveryException("Access token STOMP expirado.");
        }
        AuthenticatedUser current = sessionService.authenticate(principal.login(), principal.sid());
        if (!principal.login().equals(current.login()) || !principal.sid().equals(current.sid())) {
            throw new MessageDeliveryException("Sesión STOMP no autorizada.");
        }
        authorizeNotificationRole(principal.login());
    }

    private void authorizeNotificationRole(String login) {
        List<GrantedAuthority> authorities = userAuthorityService.loadAuthorities(login);
        boolean canReadNotifications = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> OWNER_ROLE.equals(role) || TENANT_ROLE.equals(role));
        if (!canReadNotifications) {
            throw new MessageDeliveryException("Rol sin acceso a notificaciones.");
        }
    }

}
