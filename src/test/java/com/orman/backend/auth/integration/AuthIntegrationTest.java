package com.orman.backend.auth.integration;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.Map;
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
class AuthIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SesionUsuarioRepository sesionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void mobileLoginPersistsOnlyHashAndIssuesMinimalJwt() {
        UsuarioResponse usuario = createUsuario("AUTH-101", "Usuario.Auth", "clave-ficticia");

        AuthResult result = authService.login(request(" Usuario.Auth ", " mobile-1 ", " Samsung ", ClientType.MOBILE));

        SesionUsuario persisted = sesionRepository.findById(result.response().sid()).orElseThrow();
        JwtService.JwtClaims claims = jwtService.validateAndExtract(result.response().accessToken());
        Usuario storedUser = usuarioRepository.findById(usuario.login()).orElseThrow();
        assertThat(result.response().refreshToken()).isEqualTo(result.refreshToken());
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.response().expiresIn()).isEqualTo(900);
        assertThat(persisted.getRefreshTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(persisted.getRefreshTokenHash()).hasSize(43);
        assertThat(persisted.getDeviceId()).isEqualTo("mobile-1");
        assertThat(storedUser.getUltimoAcceso()).isNotNull();
        assertThat(passwordEncoder.matches("clave-ficticia", storedUser.getPasswd())).isTrue();
        assertThat(claims.subject()).isEqualTo("Usuario.Auth");
        assertThat(claims.sid()).isEqualTo(persisted.getSid());
        assertThat(claims.expiresAt()).isEqualTo(claims.issuedAt().plusSeconds(900));
        assertThat(result.response().getClass().getRecordComponents()).extracting(component -> component.getName())
                .doesNotContain("password", "passwd", "hash", "persona", "roles");
    }

    @Test
    void webAndMobileCoexistAndSameDeviceReplacesOnlyItsSession() {
        createUsuario("AUTH-102", "multi.device", "clave-ficticia");
        AuthResult webFirst = authService.login(request("multi.device", "browser-1", "Chrome", ClientType.WEB));
        AuthResult mobile = authService.login(request("multi.device", "phone-1", "Android", ClientType.MOBILE));
        AuthResult webSecond = authService.login(request("multi.device", "browser-1", "Chrome", ClientType.WEB));

        SesionUsuario replaced = sesionRepository.findById(webFirst.response().sid()).orElseThrow();
        assertThat(replaced.getMotivoRevocacion()).isEqualTo(RevocationReason.REPLACED_BY_NEW_LOGIN);
        assertThat(sesionRepository.findById(mobile.response().sid()).orElseThrow().isRevoked()).isFalse();
        assertThat(sesionRepository.findById(webSecond.response().sid()).orElseThrow().isRevoked()).isFalse();
        assertThat(sesionRepository.findAllByUsuarioLoginAndFechaRevocacionIsNull("multi.device")).hasSize(2);
        assertThat(webSecond.response().refreshToken()).isNull();
        assertThat(webSecond.refreshToken()).isNotBlank();
    }

    @Test
    void refreshRotatesAndReuseRevokesSession() {
        createUsuario("AUTH-103", "refresh.user", "clave-ficticia");
        AuthResult login = authService.login(request("refresh.user", "phone", "Android", ClientType.MOBILE));

        AuthResult refreshed = authService.refresh(login.refreshToken(), ClientType.MOBILE);
        SesionUsuario rotated = sesionRepository.findById(login.response().sid()).orElseThrow();
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThat(rotated.getRefreshTokenVersion()).isEqualTo(2);
        assertThat(rotated.getRefreshTokenHash()).isNotEqualTo(login.refreshToken());
        assertThat(rotated.getUltimoUso()).isAfterOrEqualTo(rotated.getFechaCreacion());

        assertThatThrownBy(() -> authService.refresh(login.refreshToken(), ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(sesionRepository.findById(login.response().sid()).orElseThrow().getMotivoRevocacion())
                .isEqualTo(RevocationReason.REFRESH_REUSE);
        assertThatThrownBy(() -> authService.refresh(refreshed.refreshToken(), ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsMalformedUnknownRevokedAndInactiveRefreshSecurely() {
        createUsuario("AUTH-104", "invalid.refresh", "clave-ficticia");
        AuthResult login = authService.login(request("invalid.refresh", "phone", "Android", ClientType.MOBILE));

        assertThatThrownBy(() -> authService.refresh("malformed", ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh("b32280c6-c894-4520-a9c0-acb57d30a8cc.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        usuarioService.deactivate("invalid.refresh");
        assertThatThrownBy(() -> authService.refresh(login.refreshToken(), ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsInvalidCredentialsAndKeepsCaseSensitivity() {
        UsuarioResponse usuario = createUsuario("AUTH-105", "Usuario.Case", "clave-ficticia");
        assertThatThrownBy(() -> authService.login(request("usuario.case", "x", "Phone", ClientType.MOBILE)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest(usuario.login(), " clave-ficticia ",
                "x", "Phone", ClientType.MOBILE))).isInstanceOf(InvalidCredentialsException.class);
        assertThat(usuarioRepository.findById(usuario.login()).orElseThrow().getUltimoAcceso()).isNull();
    }

    @Test
    void validatesFlywayV6SchemaConstraintsAndIndexes() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1','2','3','4','5','6') AND success",
                Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("6");
        Map<String, Object> loginColumn = jdbcTemplate.queryForMap("""
                SELECT data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'sesiones_usuario' AND column_name = 'login'
                """);
        assertThat(loginColumn.get("data_type")).isEqualTo("character varying");
        assertThat(loginColumn.get("character_maximum_length")).isEqualTo(30);
        assertThat(loginColumn.get("is_nullable")).isEqualTo("NO");
        assertThat(jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conrelid = 'sesiones_usuario'::regclass
                """, String.class)).contains("pk_sesiones_usuario", "fk_sesiones_usuario_login",
                "ck_sesiones_usuario_client_type", "ck_sesiones_usuario_refresh_token_version",
                "ck_sesiones_usuario_revocacion");
        assertThat(jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() AND tablename = 'sesiones_usuario'
                """, String.class)).contains("ix_sesiones_usuario_login", "uk_sesiones_usuario_login_device_active");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT confdeltype = 'c' FROM pg_constraint
                WHERE conname = 'fk_sesiones_usuario_login'
                """, Boolean.class)).isTrue();
    }

    private LoginRequest request(String login, String deviceId, String deviceName, ClientType type) {
        return new LoginRequest(login, "clave-ficticia", deviceId, deviceName, type);
    }

    private UsuarioResponse createUsuario(String ci, String login, String password) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona ficticia", null, null,
                "F", null, null, "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, password, null, persona.codper()));
    }
}
