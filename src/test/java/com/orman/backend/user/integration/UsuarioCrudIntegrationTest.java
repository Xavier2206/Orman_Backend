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

    private static final String SEARCH_TERM = "RemoteXavier2026";

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
                .containsExactly("contrato_archivos", "contratos", "cuentas_pago", "cuotas", "flyway_schema_history", "menus", "mepro", "notificaciones", "otp_challenges", "pago_comprobantes", "pagos", "personas", "procesos", "propiedades", "recibos", "roles", "rolme", "rolusu", "sesiones_usuario", "unidad_fotos", "unidades", "usuarios");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13') AND success", Integer.class))
                .isEqualTo(13);
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

    @Test
    void searchesUsuariosGloballyByLoginAndPersonaNamesWithPagination() {
        PersonaResponse loginMatch = createPersona("TUSR-SRCH-01", "Persona neutra", null, null);
        PersonaResponse nombreMatch = createPersona("TUSR-SRCH-02", SEARCH_TERM, null, null);
        PersonaResponse apMatch = createPersona("TUSR-SRCH-03", "Persona neutra", SEARCH_TERM, null);
        PersonaResponse amMatch = createPersona("TUSR-SRCH-04", "Persona neutra", null, SEARCH_TERM);

        usuarioService.create(new CreateUsuarioRequest("remotexavier2026.login", "clave-ficticia", null, loginMatch.codper()));
        usuarioService.create(new CreateUsuarioRequest("remotexavier2026.nombre", "clave-ficticia", null, nombreMatch.codper()));
        usuarioService.create(new CreateUsuarioRequest("remotexavier2026.ap", "clave-ficticia", null, apMatch.codper()));
        usuarioService.create(new CreateUsuarioRequest("remotexavier2026.am", "clave-ficticia", null, amMatch.codper()));

        var firstPage = usuarioService.list(" " + SEARCH_TERM + " ",
                org.springframework.data.domain.PageRequest.of(0, 2,
                        org.springframework.data.domain.Sort.by("login").ascending()));
        var secondPage = usuarioService.list(SEARCH_TERM,
                org.springframework.data.domain.PageRequest.of(1, 2,
                        org.springframework.data.domain.Sort.by("login").ascending()));

        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(4);
        assertThat(firstPage.content()).extracting(UsuarioResponse::login)
                .containsExactly("remotexavier2026.am", "remotexavier2026.ap");
        assertThat(secondPage.content()).extracting(UsuarioResponse::login)
                .containsExactly("remotexavier2026.login", "remotexavier2026.nombre");
        assertThat(secondPage.content()).filteredOn(usuario -> usuario.login().endsWith(".nombre"))
                .singleElement().satisfies(usuario -> {
                    assertThat(usuario.nombre()).isEqualTo(SEARCH_TERM);
                    assertThat(usuario.am()).isNull();
                });
    }

    @Test
    void returnsEmptyPageWhenUsuarioSearchHasNoMatches() {
        var page = usuarioService.list("sin-coincidencias", org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    private PersonaResponse createPersona(String ci) {
        return createPersona(ci, "Persona ficticia", null, null);
    }

    private PersonaResponse createPersona(String ci, String nombre, String ap, String am) {
        return personaService.create(new CreatePersonaRequest(ci, nombre, ap, am, "F", null,
                "persona@example.test", "70000000", "A", null));
    }
}
