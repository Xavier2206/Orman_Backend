package com.orman.backend.role.integration;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.dto.response.RolUsuResponse;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    @Test
    void schemaContainsApprovedTablesConstraintsDefaultsAndForeignKeys() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                String.class);
        assertThat(tables).containsExactly("flyway_schema_history", "menus", "mepro", "personas", "procesos", "roles", "rolme", "rolusu", "sesiones_usuario", "usuarios");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7') AND success = true",
                Integer.class)).isEqualTo(7);

        List<Map<String, Object>> rolesColumns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'roles'
                ORDER BY ordinal_position
                """);
        assertThat(rolesColumns).extracting(column -> column.get("column_name"))
                .containsExactly("codr", "nombre", "estado");
        assertThat(rolesColumns).extracting(column -> column.get("data_type"))
                .containsExactly("integer", "character varying", "smallint");
        assertThat(rolesColumns).extracting(column -> column.get("character_maximum_length"))
                .containsExactly(null, 50, null);
        assertThat(rolesColumns).extracting(column -> column.get("is_nullable"))
                .containsExactly("NO", "NO", "NO");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'roles' AND column_name = 'estado'", String.class))
                .isEqualTo("1");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'roles'::regclass ORDER BY conname", String.class))
                .containsExactlyInAnyOrder("pk_roles", "uk_roles_nombre", "ck_roles_estado");

        List<Map<String, Object>> rolUsuColumns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'rolusu'
                ORDER BY ordinal_position
                """);
        assertThat(rolUsuColumns).extracting(column -> column.get("column_name"))
                .containsExactly("login", "codr", "fecha_asignacion");
        assertThat(rolUsuColumns).extracting(column -> column.get("data_type"))
                .containsExactly("character varying", "integer", "timestamp without time zone");
        assertThat(rolUsuColumns).extracting(column -> column.get("character_maximum_length"))
                .containsExactly(30, null, null);
        assertThat(rolUsuColumns).extracting(column -> column.get("is_nullable"))
                .containsExactly("NO", "NO", "NO");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'rolusu' AND column_name = 'fecha_asignacion'", String.class))
                .contains("CURRENT_TIMESTAMP");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'rolusu'::regclass ORDER BY conname", String.class))
                .containsExactlyInAnyOrder("pk_rolusu", "fk_rolusu_login", "fk_rolusu_codr");
    }

    @Test
    void administersRolesWithNormalizationValidationAndIdempotentStateChanges() {
        RolResponse created = rolService.create(new CreateRolRequest(" test-rol-admin-state ", null));

        assertThat(created.nombre()).isEqualTo("TEST-ROL-ADMIN-STATE");
        assertThat(created.estado()).isEqualTo((short) 1);
        assertThat(rolService.get(created.codr())).isEqualTo(created);
        assertThat(rolService.list(org.springframework.data.domain.PageRequest.of(0, 20)).content())
                .extracting(RolResponse::nombre).contains("TEST-ROL-ADMIN-STATE");
        assertThat(rolService.update(created.codr(), new UpdateRolRequest(" test-rol-supervisor-state ")).nombre())
                .isEqualTo("TEST-ROL-SUPERVISOR-STATE");
        assertThat(rolService.deactivate(created.codr()).estado()).isZero();
        assertThat(rolService.deactivate(created.codr()).estado()).isZero();
        assertThat(rolService.activate(created.codr()).estado()).isEqualTo((short) 1);

        assertThatThrownBy(() -> rolService.create(new CreateRolRequest("TEST-ROL-SUPERVISOR-STATE", null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> rolService.get(999999)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO roles (nombre, estado) VALUES ('INVALIDO', 2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assignsListsPreservesAndRemovesRolesWithoutExposingSensitiveData() {
        UsuarioResponse usuario = createUsuario("TEST-ROL-USUARIO-1", "usuario.rol.1");
        RolResponse firstRol = rolService.create(new CreateRolRequest("TEST-ROL-ADMIN-ASSIGN", null));
        RolResponse secondRol = rolService.create(new CreateRolRequest("TEST-ROL-OPERADOR-ASSIGN", null));

        RolUsuResponse assigned = rolUsuService.assign(usuario.login(), firstRol.codr());
        assertThat(assigned.login()).isEqualTo(usuario.login());
        assertThat(assigned.nombreRol()).isEqualTo("TEST-ROL-ADMIN-ASSIGN");
        assertThat(assigned.fechaAsignacion()).isNotNull();
        assertThat(rolUsuService.listByUsuario(usuario.login())).extracting(RolUsuResponse::codr)
                .containsExactly(firstRol.codr());
        assertThat(rolUsuService.listByRol(firstRol.codr())).extracting(RolUsuResponse::login)
                .containsExactly(usuario.login());
        assertThat(RolUsuResponse.class.getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("passwd", "password", "hash", "persona", "correo", "telefono", "foto");

        assertThatThrownBy(() -> rolUsuService.assign(usuario.login(), firstRol.codr()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> rolUsuService.assign("inexistente", firstRol.codr()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> rolUsuService.assign(usuario.login(), 999999))
                .isInstanceOf(ResourceNotFoundException.class);

        usuarioService.deactivate(usuario.login());
        assertThat(rolUsuService.listByUsuario(usuario.login())).hasSize(1);
        rolService.deactivate(firstRol.codr());
        assertThat(rolUsuService.listByUsuario(usuario.login())).hasSize(1);
        assertThatThrownBy(() -> rolUsuService.assign(usuario.login(), firstRol.codr()))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(rolUsuService.assign(usuario.login(), secondRol.codr()).codr()).isEqualTo(secondRol.codr());

        rolUsuService.remove(usuario.login(), firstRol.codr());
        assertThat(rolUsuService.listByUsuario(usuario.login())).extracting(RolUsuResponse::codr)
                .containsExactly(secondRol.codr());
        assertThatThrownBy(() -> rolUsuService.remove(usuario.login(), firstRol.codr()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void databaseCascadesAssignmentsFromUsuario() {
        UsuarioResponse usuario = createUsuario("TEST-ROL-USUARIO-2", "usuario.rol.2");
        RolResponse rol = rolService.create(new CreateRolRequest("TEST-ROL-SUPERVISOR-CASCADE", null));
        rolUsuService.assign(usuario.login(), rol.codr());

        jdbcTemplate.update("DELETE FROM usuarios WHERE login = ?", usuario.login());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rolusu WHERE login = ?", Integer.class,
                usuario.login())).isZero();
    }

    @Test
    void databaseRestrictsDeletionOfAssignedRol() {
        UsuarioResponse usuario = createUsuario("TEST-ROL-USUARIO-3", "usuario.rol.3");
        RolResponse rol = rolService.create(new CreateRolRequest("TEST-ROL-AUDITOR-RESTRICT", null));
        rolUsuService.assign(usuario.login(), rol.codr());

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM roles WHERE codr = ?", rol.codr()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UsuarioResponse createUsuario(String ci, String login) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona ficticia", null, null,
                "F", null, null, "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, "clave-ficticia", null, persona.codper()));
    }
}
