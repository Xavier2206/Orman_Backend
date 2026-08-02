package com.orman.backend.user.mapper;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UpdateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    void mapsCreationWithoutTakingPasswordFromRequest() {
        Persona persona = persona(7);
        Usuario usuario = mapper.toEntity(new CreateUsuarioRequest(" usuario.demo ", "texto-secreto", null, 7), persona);

        assertThat(usuario.getLogin()).isEqualTo("usuario.demo");
        assertThat(usuario.getPersona()).isSameAs(persona);
        assertThat(usuario.getEstado()).isNull();
        assertThat(usuario.getPasswd()).isNull();
    }

    @Test
    void updatesOnlyEstadoAndKeepsImmutableFields() {
        Usuario usuario = usuario();
        mapper.update(usuario, new UpdateUsuarioRequest((short) 0));

        assertThat(usuario.getEstado()).isZero();
        assertThat(usuario.getLogin()).isEqualTo("usuario.demo");
        assertThat(usuario.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
    }

    @Test
    void mapsResponseWithoutPasswordOrPersonaData() {
        UsuarioResponse response = mapper.toResponse(usuario());

        assertThat(response).isEqualTo(new UsuarioResponse("usuario.demo", (short) 1, 7,
                LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 2, 10, 0)));
        assertThat(UsuarioResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("password", "passwd", "hash", "persona");
    }

    private Persona persona(Integer codper) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        return persona;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setLogin("usuario.demo");
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(persona(7));
        usuario.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
        usuario.setUltimoAcceso(LocalDateTime.of(2026, 1, 2, 10, 0));
        return usuario;
    }
}
