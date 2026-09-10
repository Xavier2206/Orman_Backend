package com.orman.backend.notification.mapper;

import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacionMapperTest {

    private final NotificacionMapper mapper = new NotificacionMapper();

    @Test
    void mapsInternalNotificationWithoutExposingRecipient() {
        Usuario destinatario = new Usuario();
        destinatario.setLogin("notification.mapper");
        var entity = mapper.toEntity(destinatario, NotificacionTipo.CUOTA_VENCIDA, "Cuota vencida",
                "La cuota se encuentra vencida.", ReferenciaTipo.CUOTA, 7);

        assertThat(entity.getDestinatario()).isEqualTo(destinatario);
        assertThat(entity.getFechaCreacion()).isNotNull();
        entity.setFechaLectura(LocalDateTime.of(2026, 9, 10, 12, 0));

        var response = mapper.toResponse(entity);
        assertThat(response.tipo()).isEqualTo("CUOTA_VENCIDA");
        assertThat(response.referenciaTipo()).isEqualTo("CUOTA");
        assertThat(response.leida()).isTrue();
        assertThat(response.getClass().getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("loginDestinatario", "destinatario", "passwd", "password");
    }
}
