package com.orman.backend.notification.websocket;

import com.orman.backend.auth.config.CorsProperties;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NotificacionWebSocketBrokerIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacionWebSocketBrokerIntegrationTest {

    private static final String LOGIN_A = "owner.socket.a";
    private static final String LOGIN_B = "owner.socket.b";
    private static final UUID SID_A = UUID.fromString("33000000-0000-0000-0000-000000000001");
    private static final UUID SID_B = UUID.fromString("33000000-0000-0000-0000-000000000002");
    private static final Instant EXPIRY = Instant.parse("2099-01-01T00:00:00Z");

    @LocalServerPort
    private int port;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketStompClient stompClient;
    private StompSession sessionA;
    private StompSession sessionB;

    @Test
    void authenticatedUsersReceiveOnlyTheirPrivateBrokerMessagesWithoutOutboundPrincipal() throws Exception {
        BlockingQueue<String> messagesA = new LinkedBlockingQueue<>();
        BlockingQueue<String> messagesB = new LinkedBlockingQueue<>();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler());
        stompClient.start();

        sessionA = connect("token-a");
        sessionB = connect("token-b");
        subscribe(sessionA, messagesA);
        subscribe(sessionB, messagesB);
        Thread.sleep(250);

        messagingTemplate.convertAndSendToUser(LOGIN_A, "/queue/notificaciones", "payload-a");
        assertThat(messagesA.poll(5, TimeUnit.SECONDS)).isEqualTo("payload-a");
        assertThat(messagesB.poll(300, TimeUnit.MILLISECONDS)).isNull();

        messagingTemplate.convertAndSendToUser(LOGIN_B, "/queue/notificaciones", "payload-b");
        assertThat(messagesB.poll(5, TimeUnit.SECONDS)).isEqualTo("payload-b");
        assertThat(messagesA.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @AfterEach
    void closeClients() {
        if (sessionA != null && sessionA.isConnected()) {
            sessionA.disconnect();
        }
        if (sessionB != null && sessionB.isConnected()) {
            sessionB.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        WebSocketHttpHeaders webSocketHeaders = new WebSocketHttpHeaders();
        webSocketHeaders.setOrigin("http://localhost:4200");
        return stompClient.connectAsync("ws://localhost:" + port + "/ws", webSocketHeaders, connectHeaders,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, BlockingQueue<String> messages) {
        session.subscribe(NotificacionStompChannelInterceptor.PRIVATE_NOTIFICATION_SUBSCRIPTION,
                new QueueFrameHandler(messages));
    }

    private static final class QueueFrameHandler implements StompFrameHandler {

        private final BlockingQueue<String> messages;

        private QueueFrameHandler(BlockingQueue<String> messages) {
            this.messages = messages;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            messages.add((String) payload);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration",
            "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration"
    })
    @Import({NotificacionWebSocketConfiguration.class, NotificacionWebSocketSecurityConfiguration.class})
    static class TestApplication {

        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:4200"));
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        JwtService jwtService() {
            return new JwtService() {
                @Override
                public String generateAccessToken(String login, UUID sid) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public JwtClaims validateAndExtract(String token) {
                    return switch (token) {
                        case "token-a" -> new JwtClaims(LOGIN_A, SID_A, "test", Instant.EPOCH, EXPIRY);
                        case "token-b" -> new JwtClaims(LOGIN_B, SID_B, "test", Instant.EPOCH, EXPIRY);
                        default -> throw new InvalidJwtException();
                    };
                }
            };
        }

        @Bean
        SessionService sessionService() {
            return new SessionService() {
                private final Map<String, UUID> sessions = Map.of(LOGIN_A, SID_A, LOGIN_B, SID_B);

                @Override
                public AuthenticatedUser authenticate(String login, UUID sid) {
                    if (!sid.equals(sessions.get(login))) {
                        throw new InvalidJwtException();
                    }
                    return new AuthenticatedUser(login, sid);
                }

                @Override public void logout(AuthenticatedUser user) { throw new UnsupportedOperationException(); }
                @Override public void logoutAll(AuthenticatedUser user) { throw new UnsupportedOperationException(); }
                @Override public List<com.orman.backend.auth.dto.response.SessionResponse> list(
                        AuthenticatedUser user) { throw new UnsupportedOperationException(); }
                @Override public void revoke(AuthenticatedUser user, UUID sid) {
                    throw new UnsupportedOperationException();
                }
                @Override public void revokeAll(String login,
                        com.orman.backend.auth.model.RevocationReason reason) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Bean
        UserAuthorityService userAuthorityService() {
            return login -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_PROPIETARIO"));
        }
    }
}
