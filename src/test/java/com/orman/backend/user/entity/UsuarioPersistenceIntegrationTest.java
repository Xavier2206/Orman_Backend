package com.orman.backend.user.entity;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class UsuarioPersistenceIntegrationTest {

    private static final String FIXTURE_HASH = "$2a$TEST_FIXTURE_HASH_NOT_A_REAL_CREDENTIAL";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesUsuariosWithApprovedSchemaAndConstraints() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3') AND success = true",
                Integer.class)).isEqualTo(3);

        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'usuarios'
                ORDER BY ordinal_position
                """);

        assertThat(columns).extracting(column -> column.get("column_name"))
                .containsExactly("login", "passwd", "estado", "codper", "fecha_creacion", "ultimo_acceso");
        assertThat(columns).extracting(column -> column.get("data_type"))
                .containsExactly("character varying", "character varying", "smallint", "integer", "timestamp without time zone", "timestamp without time zone");
        assertThat(columns).extracting(column -> column.get("character_maximum_length"))
                .containsExactly(30, 255, null, null, null, null);
        assertThat(columns).extracting(column -> column.get("is_nullable"))
                .containsExactly("NO", "NO", "NO", "NO", "NO", "YES");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'estado'", String.class))
                .isEqualTo("1");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'usuarios' AND column_name = 'fecha_creacion'", String.class))
                .contains("CURRENT_TIMESTAMP");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'usuarios'::regclass ORDER BY conname", String.class))
                .containsExactlyInAnyOrder("pk_usuarios", "uk_usuarios_codper", "ck_usuarios_estado", "fk_usuarios_personas");
        assertThat(jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'usuarios' ORDER BY indexname", String.class))
                .containsExactly("pk_usuarios", "uk_usuarios_codper");
    }

    @Test
    void persistsReadsAndUsesPostgreSqlDefaults() {
        Persona persona = persistPersona("TEST-USER-DEFAULTS");
        Usuario usuario = new Usuario();
        usuario.setLogin("usuario.defaults");
        usuario.setPasswd(FIXTURE_HASH);
        usuario.setPersona(persona);

        usuarioRepository.saveAndFlush(usuario);
        entityManager.clear();

        Usuario persisted = usuarioRepository.findById("usuario.defaults").orElseThrow();
        assertThat(persisted.getEstado()).isEqualTo((short) 1);
        assertThat(persisted.getFechaCreacion()).isInstanceOf(LocalDateTime.class);
        assertThat(persisted.getUltimoAcceso()).isNull();
        assertThat(persisted.getPersona().getCodper()).isEqualTo(persona.getCodper());
    }

    @Test
    void onlyOneUsuarioCanReferenceTheSamePersona() {
        Persona persona = persistPersona("TEST-USER-UNIQUE");
        persistUsuario("usuario.unico.1", persona);

        assertThatThrownBy(() -> persistUsuario("usuario.unico.2", persona))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void estadoAcceptsZeroAndRejectsOtherValues() {
        Persona persona = persistPersona("TEST-USER-ESTADO");
        jdbcTemplate.update("INSERT INTO usuarios (login, passwd, estado, codper) VALUES (?, ?, 0, ?)",
                "usuario.inactivo", FIXTURE_HASH, persona.getCodper());

        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM usuarios WHERE login = ?", Short.class, "usuario.inactivo"))
                .isEqualTo((short) 0);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (login, passwd, estado, codper) VALUES (?, ?, 2, ?)",
                "usuario.estado.invalido", FIXTURE_HASH, persistPersona("TEST-USER-STINV").getCodper()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void requiredColumnsAndForeignKeyAreEnforced() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (login, passwd, codper) VALUES (?, ?, ?)",
                "usuario.persona.inexistente", FIXTURE_HASH, 999999999))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void passwdCannotBeNull() {
        Persona persona = persistPersona("TEST-USER-SIN-PASSWD");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (login, codper) VALUES (?, ?)", "usuario.sin.passwd", persona.getCodper()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void codperCannotBeNull() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (login, passwd) VALUES (?, ?)", "usuario.sin.persona", FIXTURE_HASH))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void personaWithUsuarioCannotBeDeletedBecauseOfRestrict() {
        Persona persona = persistPersona("TEST-USER-RESTRICT");
        persistUsuario("usuario.restrict", persona);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM personas WHERE codper = ?", persona.getCodper()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUsuarioDoesNotDeletePersona() {
        Persona persona = persistPersona("TEST-USER-NO-CASCADE");
        persistUsuario("usuario.sin.cascada", persona);

        usuarioRepository.deleteById("usuario.sin.cascada");
        usuarioRepository.flush();
        entityManager.clear();

        assertThat(usuarioRepository.findById("usuario.sin.cascada")).isEmpty();
        assertThat(personaRepository.findById(persona.getCodper())).isPresent();
    }

    private Persona persistPersona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona ficticia");
        persona.setGenero('F');
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private void persistUsuario(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd(FIXTURE_HASH);
        usuario.setPersona(persona);
        usuarioRepository.saveAndFlush(usuario);
    }
}
