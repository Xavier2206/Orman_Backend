package com.orman.backend.notification.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.mapper.NotificacionMapper;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.user.entity.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceBusinessTimeTest {

    @Mock private NotificacionRepository repository;

    @Test
    void marksNotificationReadAtBoliviaWallTimeAndReturnsOffset() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T16:30:00Z"), ZoneOffset.UTC);
        Usuario recipient = new Usuario();
        recipient.setLogin("tenant.time");
        NotificacionEntity notification = new NotificacionEntity();
        ReflectionTestUtils.setField(notification, "codnot", 29L);
        notification.setDestinatario(recipient);
        notification.setTipo(NotificacionTipo.CUOTA_VENCIDA);
        notification.setTitulo("Cuota vencida");
        notification.setMensaje("La cuota está vencida.");
        notification.setReferenciaTipo(ReferenciaTipo.CUOTA);
        notification.setReferenciaId(8);
        notification.setFechaCreacion(LocalDateTime.of(2026, 9, 30, 12, 25));
        Authentication authentication = new TestingAuthenticationToken(
                new AuthenticatedUser("tenant.time", UUID.randomUUID()), null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_INQUILINO")));
        when(repository.findOwnForUpdate(29L, "tenant.time")).thenReturn(Optional.of(notification));

        NotificacionServiceImpl service = new NotificacionServiceImpl(repository, new NotificacionMapper(clock), clock);
        var response = service.markAsRead(29L, authentication);

        assertThat(notification.getFechaLectura()).isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 30));
        assertThat(response.fechaCreacion()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 25, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.fechaLectura()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
    }
}
