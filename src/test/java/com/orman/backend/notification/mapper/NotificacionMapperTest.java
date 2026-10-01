package com.orman.backend.notification.mapper;

import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.user.entity.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacionMapperTest {

    private final NotificacionMapper mapper = new NotificacionMapper(
            Clock.fixed(Instant.parse("2026-09-30T16:25:00Z"), ZoneOffset.UTC));

    @Test
    void mapsInternalNotificationWithoutExposingRecipient() {
        Usuario destinatario = new Usuario();
        destinatario.setLogin("notification.mapper");
        var entity = mapper.toEntity(destinatario, NotificacionTipo.CUOTA_VENCIDA, "Cuota vencida",
                "La cuota se encuentra vencida.", ReferenciaTipo.CUOTA, 7);

        assertThat(entity.getDestinatario()).isEqualTo(destinatario);
        assertThat(entity.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 25));
        entity.setFechaLectura(LocalDateTime.of(2026, 9, 30, 12, 30));

        var response = mapper.toResponse(entity);
        assertThat(response.tipo()).isEqualTo("CUOTA_VENCIDA");
        assertThat(response.referenciaTipo()).isEqualTo("CUOTA");
        assertThat(response.leida()).isTrue();
        assertThat(response.fechaCreacion()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 25, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.fechaLectura()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 30, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.getClass().getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("loginDestinatario", "destinatario", "passwd", "password");
    }
}
