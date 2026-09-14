package com.orman.backend.person.entity;

import com.orman.backend.person.repository.PersonaRepository;
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
class PersonaPersistenceIntegrationTest {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesEightVersionsAndCreatesPersonasUsuariosRolesAndRolUsu() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                String.class);

        assertThat(tables).containsExactly("contrato_archivos", "contratos", "cuentas_pago", "cuotas", "flyway_schema_history", "menus", "mepro", "notificaciones", "otp_challenges", "pago_comprobantes", "pagos", "personas", "procesos", "propiedades", "recibos", "roles", "rolme", "rolusu", "sesiones_usuario", "unidad_fotos", "unidades", "usuarios");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14') AND success = true",
                Integer.class)).isEqualTo(14);
    }

    @Test
    void schemaMatchesApprovedColumnsDefaultsConstraintsAndIndexes() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'personas'
                ORDER BY ordinal_position
                """);

        assertThat(columns).extracting(column -> column.get("column_name"))
                .containsExactly("codper", "ci", "nombre", "ap", "am", "genero", "estado", "correo", "telefono", "tipo_persona", "foto", "fecha_registro");
        assertThat(columns).extracting(column -> column.get("data_type"))
                .containsExactly("integer", "character varying", "character varying", "character varying", "character varying", "character", "smallint", "character varying", "character varying", "character", "character varying", "timestamp without time zone");
        assertThat(columns).extracting(column -> column.get("character_maximum_length"))
                .containsExactly(null, 20, 60, 40, 40, 1, null, 100, 20, 1, 255, null);
        assertThat(columns).extracting(column -> column.get("is_nullable"))
                .containsExactly("NO", "NO", "NO", "YES", "YES", "NO", "NO", "NO", "NO", "NO", "YES", "NO");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'personas' AND column_name = 'estado'", String.class))
                .isEqualTo("1");
        assertThat(jdbcTemplate.queryForObject("SELECT column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'personas' AND column_name = 'fecha_registro'", String.class))
                .contains("CURRENT_TIMESTAMP");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'personas'::regclass ORDER BY conname", String.class))
                .containsExactlyInAnyOrder("pk_personas", "uk_personas_ci", "ck_personas_genero", "ck_personas_estado", "ck_personas_tipo_persona");
        assertThat(jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'personas' ORDER BY indexname", String.class))
                .containsExactly("pk_personas", "uk_personas_ci");
    }

    @Test
    void persistsReadsAndUsesPostgreSqlDefaults() {
        Persona persona = new Persona();
        persona.setCi("TEST-CI-VALIDO-01");
        persona.setNombre("Persona Ficticia");
        persona.setGenero('F');
        persona.setCorreo("persistencia@example.test");
        persona.setTelefono("70000001");
        persona.setTipoPersona('A');

        Persona saved = personaRepository.saveAndFlush(persona);
        assertThat(saved.getCodper()).isNotNull();
        Integer codper = saved.getCodper();
        entityManager.clear();

        Persona persisted = personaRepository.findById(codper).orElseThrow();
        assertThat(persisted.getEstado()).isEqualTo((short) 1);
        assertThat(persisted.getFechaRegistro()).isInstanceOf(LocalDateTime.class);
        assertThat(persisted.getAp()).isNull();
        assertThat(persisted.getAm()).isNull();
        assertThat(persisted.getCorreo()).isEqualTo("persistencia@example.test");
        assertThat(persisted.getFoto()).isNull();
    }

    @Test
    void ciIsUnique() {
        insertValid("TEST-CI-DUPLICADO", 'F', (short) 1, 'A');

        assertThatThrownBy(() -> insertValid("TEST-CI-DUPLICADO", 'F', (short) 1, 'A'))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ciNombreAndTelefonoAreRequired() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO personas (nombre, genero, telefono, tipo_persona) VALUES ('Nombre ficticio', 'F', '70000002', 'A')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nombreIsRequired() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO personas (ci, genero, telefono, tipo_persona) VALUES ('TEST-CI-SIN-NOMBRE', 'F', '70000003', 'A')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void telefonoIsRequired() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO personas (ci, nombre, genero, tipo_persona) VALUES ('TEST-CI-SIN-TELEFONO', 'Nombre ficticio', 'F', 'A')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void correoIsRequired() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO personas (ci, nombre, genero, telefono, tipo_persona) VALUES ('TEST-CI-SIN-CORREO', 'Nombre ficticio', 'F', '70000004', 'A')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void generoAcceptsOnlyApprovedValues() {
        assertThatThrownBy(() -> insertValid("TEST-CI-GENERO", 'X', (short) 1, 'A'))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void estadoAcceptsOnlyApprovedValues() {
        assertThatThrownBy(() -> insertValid("TEST-CI-ESTADO", 'F', (short) 2, 'A'))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tipoPersonaAcceptsOnlyApprovedValues() {
        assertThatThrownBy(() -> insertValid("TEST-CI-TIPO", 'F', (short) 1, 'X'))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertValid(String ci, Character genero, short estado, Character tipoPersona) {
        jdbcTemplate.update(
                "INSERT INTO personas (ci, nombre, genero, estado, correo, telefono, tipo_persona) VALUES (?, 'Nombre ficticio', ?, ?, 'persistencia@example.test', '70000000', ?)",
                ci, genero, estado, tipoPersona);
    }
}
