package com.orman.backend.user.integration;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class UsuarioCrudIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void administersUsuarioWithBcryptWithoutChangingSchema() {
        PersonaResponse persona = createPersona("TEST-USUARIO-CRUD-1");
        UsuarioResponse created = usuarioService.create(new CreateUsuarioRequest(" usuario.integracion ",
                "clave-ficticia", null, persona.codper()));

        Usuario persisted = usuarioRepository.findById("usuario.integracion").orElseThrow();
        assertThat(created.estado()).isEqualTo((short) 1);
        assertThat(created.fechaCreacion()).isNotNull();
        assertThat(persisted.getPasswd()).isNotEqualTo("clave-ficticia");
        assertThat(passwordEncoder.matches("clave-ficticia", persisted.getPasswd())).isTrue();
        assertThat(passwordEncoder.matches("otra-clave", persisted.getPasswd())).isFalse();
        assertThat(usuarioService.get("usuario.integracion")).isEqualTo(created);
        assertThat(usuarioService.list(org.springframework.data.domain.PageRequest.of(0, 20)).content())
                .extracting(UsuarioResponse::login).contains("usuario.integracion");

        assertThat(usuarioService.deactivate("usuario.integracion").estado()).isZero();
        assertThat(usuarioRepository.findById("usuario.integracion")).isPresent();
        assertThat(usuarioService.activate("usuario.integracion").estado()).isEqualTo((short) 1);
        usuarioService.changePassword("usuario.integracion", new ChangePasswordRequest("nueva-clave"));
        String newHash = usuarioRepository.findById("usuario.integracion").orElseThrow().getPasswd();
        assertThat(passwordEncoder.matches("clave-ficticia", newHash)).isFalse();
        assertThat(passwordEncoder.matches("nueva-clave", newHash)).isTrue();

        assertThat(UsuarioResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("passwd", "password", "hash");
        assertThat(jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name", String.class))
                .containsExactly("flyway_schema_history", "personas", "roles", "rolusu", "sesiones_usuario", "usuarios");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6') AND success", Integer.class))
                .isEqualTo(6);
    }

    @Test
    void rejectsDuplicateLoginDuplicatePersonaAndMissingPersona() {
        PersonaResponse persona = createPersona("TEST-USUARIO-CRUD-2");
        usuarioService.create(new CreateUsuarioRequest("usuario.primero", "clave-ficticia", null, persona.codper()));

        assertThatThrownBy(() -> usuarioService.create(new CreateUsuarioRequest("usuario.primero", "clave-ficticia", null, persona.codper())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> usuarioService.create(new CreateUsuarioRequest("usuario.segundo", "clave-ficticia", null, persona.codper())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> usuarioService.create(new CreateUsuarioRequest("usuario.sin-persona", "clave-ficticia", null, 99999999)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PersonaResponse createPersona(String ci) {
        return personaService.create(new CreatePersonaRequest(ci, "Persona ficticia", null, null, "F", null,
                null, "70000000", "A", null));
    }
}
