package com.orman.backend.auth.entity;

import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class SesionUsuarioPersistenceIntegrationTest {

    private static final String FIXTURE_HASH = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    @Autowired private SesionUsuarioRepository sesionRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void v6CreatesExactColumnsDefaultsAndPartialUniqueIndex() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'sesiones_usuario'
                ORDER BY ordinal_position
                """);
        assertThat(columns).extracting(row -> row.get("column_name")).containsExactly(
                "sid", "login", "refresh_token_hash", "refresh_token_version", "device_id", "device_name",
                "client_type", "fecha_creacion", "fecha_expiracion", "ultimo_uso", "fecha_revocacion", "motivo_revocacion");
        assertThat(columns).extracting(row -> row.get("data_type")).containsExactly(
                "uuid", "character varying", "character varying", "integer", "character varying",
                "character varying", "character varying", "timestamp without time zone", "timestamp without time zone",
                "timestamp without time zone", "timestamp without time zone", "character varying");
        assertThat(columns).extracting(row -> row.get("is_nullable")).containsExactly(
                "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "YES", "YES");
        assertThat(columns.get(3).get("column_default")).isEqualTo("1");
        assertThat(columns.get(7).get("column_default").toString()).contains("CURRENT_TIMESTAMP");
        assertThat(columns.get(9).get("column_default").toString()).contains("CURRENT_TIMESTAMP");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'uk_sesiones_usuario_login_device_active'
                """, String.class)).contains("UNIQUE", "login", "device_id", "fecha_revocacion IS NULL");
    }

    @Test
    void persistsSessionWithLazyUsuarioAndWithoutCascade() {
        Usuario usuario = persistUsuario("session.lazy", "SESSION-LAZY");
        SesionUsuario session = new SesionUsuario(UUID.randomUUID(), usuario, FIXTURE_HASH, "device", "Browser",
                ClientType.WEB, LocalDateTime.now(), LocalDateTime.now().plusDays(30));
        UUID sid = sesionRepository.saveAndFlush(session).getSid();
        entityManager.clear();

        SesionUsuario persisted = sesionRepository.findById(sid).orElseThrow();
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(persisted, "usuario")).isFalse();
        assertThat(persisted.getRefreshTokenVersion()).isEqualTo(1);
        sesionRepository.delete(persisted);
        sesionRepository.flush();
        assertThat(usuarioRepository.findById("session.lazy")).isPresent();
    }

    @Test
    void partialIndexAllowsRevokedHistoryButRejectsTwoActiveSessions() {
        Usuario usuario = persistUsuario("session.unique", "SESSION-UNIQUE");
        LocalDateTime now = LocalDateTime.now();
        SesionUsuario first = new SesionUsuario(UUID.randomUUID(), usuario, FIXTURE_HASH, "same-device", "Phone",
                ClientType.MOBILE, now, now.plusDays(30));
        sesionRepository.saveAndFlush(first);

        SesionUsuario duplicate = new SesionUsuario(UUID.randomUUID(), usuario, FIXTURE_HASH, "same-device", "Phone",
                ClientType.MOBILE, now, now.plusDays(30));
        assertThatThrownBy(() -> sesionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void clientTypeCheckIsEnforced() {
        Usuario usuario = persistUsuario("session.client", "SESSION-CLIENT");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO sesiones_usuario
                    (sid, login, refresh_token_hash, device_id, device_name, client_type, fecha_expiracion)
                VALUES (?, ?, ?, ?, ?, 'DESKTOP', ?)
                """, UUID.randomUUID(), usuario.getLogin(), FIXTURE_HASH, "device", "Desktop", LocalDateTime.now().plusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void versionCheckIsEnforced() {
        Usuario usuario = persistUsuario("session.checks", "SESSION-CHECKS");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO sesiones_usuario
                    (sid, login, refresh_token_hash, refresh_token_version, device_id, device_name, client_type,
                     fecha_expiracion)
                VALUES (?, ?, ?, 0, ?, ?, 'WEB', ?)
                """, UUID.randomUUID(), usuario.getLogin(), FIXTURE_HASH, "device", "Browser",
                LocalDateTime.now().plusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revocationCheckRequiresDateAndReasonTogether() {
        Usuario usuario = persistUsuario("session.revocation", "SESSION-REVOCATION");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO sesiones_usuario
                    (sid, login, refresh_token_hash, device_id, device_name, client_type,
                     fecha_expiracion, fecha_revocacion)
                VALUES (?, ?, ?, ?, ?, 'WEB', ?, ?)
                """, UUID.randomUUID(), usuario.getLogin(), FIXTURE_HASH, "device", "Browser",
                LocalDateTime.now().plusDays(1), LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUsuarioCascadesOnlyItsSessions() {
        Usuario usuario = persistUsuario("session.cascade", "SESSION-CASCADE");
        UUID sid = sesionRepository.saveAndFlush(new SesionUsuario(UUID.randomUUID(), usuario, FIXTURE_HASH,
                "device", "Phone", ClientType.MOBILE, LocalDateTime.now(), LocalDateTime.now().plusDays(30))).getSid();
        Integer codper = usuario.getPersona().getCodper();

        entityManager.clear();
        jdbcTemplate.update("DELETE FROM usuarios WHERE login = ?", usuario.getLogin());
        entityManager.clear();

        assertThat(sesionRepository.findById(sid)).isEmpty();
        assertThat(personaRepository.findById(codper)).isPresent();
    }

    private Usuario persistUsuario(String login, String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona ficticia");
        persona.setGenero('F');
        persona.setCorreo("sesion@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        persona = personaRepository.saveAndFlush(persona);

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd("$2a$TEST_FIXTURE_HASH_NOT_A_REAL_CREDENTIAL");
        usuario.setPersona(persona);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
