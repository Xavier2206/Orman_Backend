package com.orman.backend.role.integration;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.dto.response.RolUsuResponse;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.service.UsuarioService;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class RolIntegrationTest {

    @Autowired private RolService rolService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private Flyway flyway;

    @Test
    void migrationLeavesExactlyTwoActiveRolesAndNoOtpTable() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("27");
        assertThat(jdbcTemplate.queryForList("SELECT nombre FROM roles ORDER BY nombre", String.class))
                .containsExactly("INQUILINO", "PROPIETARIO");
        assertThat(jdbcTemplate.queryForList("SELECT estado FROM roles", Short.class))
                .containsExactlyInAnyOrder((short) 1, (short) 1);
        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.otp_challenges')", String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.sesiones_usuario')", String.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.dispositivos_push')", String.class)).isNotNull();
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'roles'::regclass", String.class))
                .contains("ck_roles_catalogo_final", "pk_roles", "uk_roles_nombre");
    }

    @Test
    void fixedCatalogueSupportsQueriesAndRejectsDatabaseMutations() {
        List<RolResponse> roles = rolService.list(PageRequest.of(0, 20)).content();
        assertThat(roles).extracting(RolResponse::nombre).containsExactly("INQUILINO", "PROPIETARIO");
        assertThat(rolService.resumen().totalRoles()).isEqualTo(2);
        assertThat(rolService.resumen().activos()).isEqualTo(2);
        assertThat(rolService.list("prop", (short) 1, PageRequest.of(0, 10)).content())
                .extracting(RolResponse::nombre).containsExactly("PROPIETARIO");
        assertThatThrownBy(() -> rolService.get(999999)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO roles (nombre) VALUES ('ADMINISTRADOR')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsRenamingFixedRole() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE roles SET nombre = 'ELECTRICISTA' WHERE nombre = 'INQUILINO'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDeactivatingFixedRole() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE roles SET estado = 0 WHERE nombre = 'INQUILINO'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assignsOnlyCatalogueRolesAndPreservesForeignKeyProtection() {
        var person = personaService.create(new CreatePersonaRequest("ROL-FINAL-1", "Persona ficticia", null, null,
                "F", null, "persona.role@example.test", "70000000", "A", null));
        var user = usuarioService.create(new CreateUsuarioRequest("rol.final.user", "clave-ficticia", null, person.codper()));
        int inquilino = rolService.list("INQUILINO", null, PageRequest.of(0, 10)).content().getFirst().codr();
        RolUsuResponse assigned = rolUsuService.assign(user.login(), inquilino);
        assertThat(assigned.nombreRol()).isEqualTo("INQUILINO");
        assertThat(assigned.fechaAsignacion().getOffset().getId()).isEqualTo("-04:00");
        assertThat(rolUsuService.listByUsuario(user.login())).extracting(RolUsuResponse::nombreRol)
                .containsExactly("INQUILINO");
        assertThatThrownBy(() -> rolUsuService.assign(user.login(), inquilino))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> rolUsuService.assign(user.login(), 999999))
                .isInstanceOf(ResourceNotFoundException.class);
        rolUsuService.remove(user.login(), inquilino);
        assertThat(rolUsuService.listByUsuario(user.login())).isEmpty();
    }
}
