package com.orman.backend.notification.websocket;

import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NotificacionWebSocketSecurityConfiguration {

    @Bean
    NotificacionStompChannelInterceptor notificacionStompChannelInterceptor(JwtService jwtService,
            SessionService sessionService, UserAuthorityService userAuthorityService, Clock clock) {
        return new NotificacionStompChannelInterceptor(jwtService, sessionService, userAuthorityService, clock);
    }

    @Bean
    NotificacionOutboundAuthorizationInterceptor notificacionOutboundAuthorizationInterceptor(
            NotificacionStompChannelInterceptor interceptor, Clock clock) {
        return new NotificacionOutboundAuthorizationInterceptor(interceptor, clock);
    }
}
