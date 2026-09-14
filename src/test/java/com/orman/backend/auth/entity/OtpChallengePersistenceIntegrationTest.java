package com.orman.backend.auth.entity;

import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.OtpChallengeStatus;
import com.orman.backend.auth.model.OtpPurpose;
import com.orman.backend.auth.repository.OtpChallengeRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
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
class OtpChallengePersistenceIntegrationTest {

    private static final String DIGEST = "A".repeat(64);

    @Autowired private OtpChallengeRepository challengeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void v9CreatesApprovedSchemaConstraintsAndPartialUniqueIndex() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1','2','3','4','5','6','7','8','9','10','11','12','13','14') AND success = true",
                Integer.class)).isEqualTo(14);
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'otp_challenges'
                ORDER BY ordinal_position
                """);
        assertThat(columns).extracting(c -> c.get("column_name")).containsExactly(
                "id", "login", "client_type", "purpose", "otp_digest", "status", "attempts", "resend_count",
                "created_at", "expires_at", "last_sent_at", "verified_at", "ip_address", "user_agent");
        assertThat(columns).extracting(c -> c.get("data_type")).containsExactly(
                "uuid", "character varying", "character varying", "character varying", "character varying",
                "character varying", "smallint", "smallint", "timestamp without time zone",
                "timestamp without time zone", "timestamp without time zone", "timestamp without time zone",
                "character varying", "text");
        assertThat(columns).extracting(c -> c.get("character_maximum_length")).containsExactly(
                null, 30, 10, 40, 64, 20, null, null, null, null, null, null, 45, null);
        assertThat(columns).extracting(c -> c.get("is_nullable")).containsExactly(
                "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "YES", "YES", "YES", "YES");
        assertThat(columns.get(6).get("column_default")).isEqualTo("0");
        assertThat(columns.get(7).get("column_default")).isEqualTo("0");
        assertThat(columns.get(8).get("column_default").toString()).contains("CURRENT_TIMESTAMP");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'otp_challenges'::regclass", String.class))
                .containsExactlyInAnyOrder("pk_otp_challenges", "fk_otp_challenges_login", "ck_otp_challenges_client_type",
                        "ck_otp_challenges_purpose", "ck_otp_challenges_status", "ck_otp_challenges_attempts",
                        "ck_otp_challenges_resend_count", "ck_otp_challenges_expiration");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes WHERE schemaname = 'public'
                AND indexname = 'uk_otp_challenges_login_client_type_purpose_pending'
                """, String.class)).contains("UNIQUE", "login", "client_type", "purpose", "WHERE", "PENDING");
    }

    @Test
    void persistsUuidValidChallengeDefaultsAndNullableMetadata() {
        Usuario usuario = persistUsuario("otp.valid", "OTP-VALID");
        UUID id = UUID.randomUUID();
        challengeRepository.saveAndFlush(new OtpChallenge(id, usuario.getLogin(), ClientType.WEB, OtpPurpose.LOGIN,
                DIGEST, OtpChallengeStatus.PENDING, LocalDateTime.now().plusMinutes(5)));
        entityManager.clear();

        OtpChallenge challenge = challengeRepository.findById(id).orElseThrow();
        assertThat(challenge.getId()).isEqualTo(id);
        assertThat(challenge.getLogin()).isEqualTo(usuario.getLogin());
        assertThat(challenge.getAttempts()).isEqualTo((short) 0);
        assertThat(challenge.getResendCount()).isEqualTo((short) 0);
        assertThat(challenge.getCreatedAt()).isNotNull();
        assertThat(challenge.getLastSentAt()).isNull();
        assertThat(challenge.getVerifiedAt()).isNull();
        assertThat(challenge.getIpAddress()).isNull();
        assertThat(challenge.getUserAgent()).isNull();
    }

    @Test
    void foreignKeyAndEnumeratedChecksAreEnforced() {
        Usuario usuario = persistUsuario("otp.enums", "OTP-ENUMS");
        insert(usuario.getLogin(), "WEB", "LOGIN", "PENDING", 0, 0, DIGEST, future());
        insert(usuario.getLogin(), "MOBILE", "LOGIN", "VERIFIED", 0, 0, DIGEST, future());
        insert(usuario.getLogin(), "WEB", "LOGIN", "LOCKED", 0, 0, DIGEST, future());
        insert(usuario.getLogin(), "WEB", "LOGIN", "CANCELLED", 0, 0, DIGEST, future());
        assertThatThrownBy(() -> insert("otp.missing", "WEB", "LOGIN", "VERIFIED", 0, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidClientTypeIsRejected() {
        Usuario usuario = persistUsuario("otp.invalid-client", "OTP-INVALID-CLIENT");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "DESKTOP", "LOGIN", "VERIFIED", 0, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidPurposeIsRejected() {
        Usuario usuario = persistUsuario("otp.invalid-purpose", "OTP-INVALID-PURPOSE");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "RESET", "VERIFIED", 0, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidStatusIsRejected() {
        Usuario usuario = persistUsuario("otp.invalid-status", "OTP-INVALID-STATUS");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "EXPIRED", 0, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countersDigestLengthAndExpirationAreEnforced() {
        Usuario usuario = persistUsuario("otp.checks", "OTP-CHECKS");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "VERIFIED", -1, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativeResendCountIsRejected() {
        Usuario usuario = persistUsuario("otp.negative-resend", "OTP-NEGATIVE-RESEND");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "VERIFIED", 0, -1, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullOrOversizedDigestIsRejected() {
        Usuario usuario = persistUsuario("otp.digest-check", "OTP-DIGEST-CHECK");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "VERIFIED", 0, 0, null, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void oversizedDigestIsRejected() {
        Usuario usuario = persistUsuario("otp.digest-length", "OTP-DIGEST-LENGTH");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "VERIFIED", 0, 0, "A".repeat(65), future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidExpirationIsRejected() {
        Usuario usuario = persistUsuario("otp.expiration", "OTP-EXPIRATION");
        LocalDateTime created = LocalDateTime.now();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO otp_challenges (id, login, client_type, purpose, otp_digest, status, created_at, expires_at)
                VALUES (?, ?, 'WEB', 'LOGIN', ?, 'VERIFIED', ?, ?)
                """, UUID.randomUUID(), usuario.getLogin(), DIGEST, created, created))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsIpv4Ipv6AndPendingUniquenessWhilePreservingHistoryAndUsuario() {
        Usuario usuario = persistUsuario("otp.unique", "OTP-UNIQUE");
        UUID pending = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO otp_challenges (id, login, client_type, purpose, otp_digest, status, expires_at, ip_address, user_agent)
                VALUES (?, ?, 'WEB', 'LOGIN', ?, 'PENDING', ?, '192.0.2.1', 'Browser')
                """, pending, usuario.getLogin(), DIGEST, future());
        UUID history = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO otp_challenges (id, login, client_type, purpose, otp_digest, status, expires_at, ip_address)
                VALUES (?, ?, 'WEB', 'LOGIN', ?, 'VERIFIED', ?, '2001:db8::1')
                """, history, usuario.getLogin(), DIGEST, future());
        assertThat(jdbcTemplate.queryForObject("SELECT ip_address FROM otp_challenges WHERE id = ?", String.class, pending))
                .isEqualTo("192.0.2.1");
        assertThat(jdbcTemplate.queryForObject("SELECT ip_address FROM otp_challenges WHERE id = ?", String.class, history))
                .isEqualTo("2001:db8::1");
        assertThatThrownBy(() -> insert(usuario.getLogin(), "WEB", "LOGIN", "PENDING", 0, 0, DIGEST, future()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingChallengeDoesNotAffectUsuarioAndPreviousTablesRemainAvailable() {
        Usuario usuario = persistUsuario("otp.delete", "OTP-DELETE");
        UUID challengeId = challengeRepository.saveAndFlush(new OtpChallenge(UUID.randomUUID(), usuario.getLogin(),
                ClientType.WEB, OtpPurpose.LOGIN, DIGEST, OtpChallengeStatus.PENDING, future())).getId();
        challengeRepository.deleteById(challengeId);
        challengeRepository.flush();
        assertThat(usuarioRepository.findById(usuario.getLogin())).isPresent();
        assertThat(jdbcTemplate.queryForObject("SELECT to_regclass('public.personas') IS NOT NULL AND to_regclass('public.usuarios') IS NOT NULL AND to_regclass('public.sesiones_usuario') IS NOT NULL", Boolean.class)).isTrue();
    }

    private void insert(String login, String clientType, String purpose, String status, int attempts, int resendCount,
            String digest, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO otp_challenges (id, login, client_type, purpose, otp_digest, status, attempts, resend_count, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), login, clientType, purpose, digest, status, attempts, resendCount, expiresAt);
    }

    private LocalDateTime future() {
        return LocalDateTime.now().plusMinutes(5);
    }

    private Usuario persistUsuario(String login, String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona ficticia");
        persona.setGenero('F');
        persona.setCorreo("otp@example.test");
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
