package com.orman.backend.auth.mapper;

import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionMapperTest {

    @Test
    void presentsUtcSessionInstantsWithTheBolivianOffset() {
        UUID sid = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 9, 29, 23, 5);
        SesionUsuario session = new SesionUsuario(sid, new Usuario(), "opaque-hash", "browser-1", "Browser",
                ClientType.WEB, created, created.plusDays(30));

        var response = new SessionMapper().toResponse(session, sid);

        assertThat(response.fechaCreacion()).isEqualTo(OffsetDateTime.of(
                2026, 9, 29, 19, 5, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.fechaExpiracion()).isEqualTo(OffsetDateTime.of(
                2026, 10, 29, 19, 5, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.ultimoUso()).isEqualTo(response.fechaCreacion());
        assertThat(response.current()).isTrue();
    }
}
