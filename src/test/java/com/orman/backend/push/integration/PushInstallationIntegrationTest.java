package com.orman.backend.push.integration;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.exception.ExpiredSessionException;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.exception.RevokedSessionException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.common.error.ErrorCode;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.push.dto.PushInstallationRequest;
import com.orman.backend.push.entity.DispositivoPushEntity;
import com.orman.backend.push.model.PushPlatform;
import com.orman.backend.push.repository.DispositivoPushRepository;
import com.orman.backend.push.service.PushInstallationService;
import com.orman.backend.push.service.PushDestination;
import com.orman.backend.push.service.impl.PushDestinationService;
import com.orman.backend.user.dto.ChangePasswordRequest;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PushInstallationIntegrationTest {

    private static final String INSTALLATION_ID = "fid-installation-test-123";
    private static final String PASSWORD = "clave-ficticia-segura";

    @Autowired private PushInstallationService pushInstallationService;
    @Autowired private PushDestinationService pushDestinationService;
    @Autowired private DispositivoPushRepository pushRepository;
    @Autowired private AuthService authService;
    @Autowired private SessionService sessionService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private PersonaService personaService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private final List<Fixture> fixtures = new ArrayList<>();

    @AfterEach
    void cleanFixtures() {
        for (Fixture fixture : fixtures) {
            jdbcTemplate.update("DELETE FROM usuarios WHERE login = ?", fixture.login());
            jdbcTemplate.update("DELETE FROM personas WHERE codper = ?", fixture.codper());
        }
        fixtures.clear();
    }

    @Test
    void v21CreatesValidatedSchemaAndHibernateStartsWithValidate() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("23");
        assertThat(jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'dispositivos_push'
                ORDER BY ordinal_position
                """, String.class)).containsExactly("coddis", "sid", "installation_id", "platform", "activo",
                "fecha_registro", "fecha_actualizacion");
        assertThat(jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint WHERE conrelid = 'dispositivos_push'::regclass
                """, String.class)).contains("pk_dispositivos_push", "fk_dispositivos_push_sid",
                "uk_dispositivos_push_sid", "uk_dispositivos_push_installation_id", "ck_dispositivos_push_platform");
        assertThat(jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname = current_schema()
                  AND tablename = 'dispositivos_push'
                """, String.class)).contains("uk_dispositivos_push_sid", "uk_dispositivos_push_installation_id");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT confdeltype = 'c' FROM pg_constraint WHERE conname = 'fk_dispositivos_push_sid'
                """, Boolean.class)).isTrue();
    }

    @Test
    void registrationIsIdempotentAndReplacingFidLeavesOnlyTheNewDestination() {
        Fixture fixture = createUser();
        AuthenticatedUser mobile = login(fixture, "phone-a", ClientType.MOBILE);

        pushInstallationService.register(mobile, request("fid-old"));
        Long coddis = pushRepository.findBySesionSid(mobile.sid()).orElseThrow().getCoddis();
        pushInstallationService.register(mobile, request("fid-old"));

        DispositivoPushEntity repeated = pushRepository.findBySesionSid(mobile.sid()).orElseThrow();
        assertThat(repeated.getCoddis()).isEqualTo(coddis);
        assertThat(repeated.isActivo()).isTrue();
        assertThat(pushCountByLogin(fixture.login())).isEqualTo(1);

        pushInstallationService.register(mobile, request("fid-new"));
        DispositivoPushEntity replaced = pushRepository.findBySesionSid(mobile.sid()).orElseThrow();
        assertThat(replaced.getInstallationId()).isEqualTo("fid-new");
        assertThat(replaced.isActivo()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE installation_id = 'fid-old'",
                Integer.class)).isZero();
    }

    @Test
    void installationCanBeReassociatedToNewSessionAndReplacesItsPreviousDestination() {
        Fixture fixture = createUser();
        AuthenticatedUser oldSession = login(fixture, "phone-a", ClientType.MOBILE);
        AuthenticatedUser newSession = login(fixture, "phone-b", ClientType.MOBILE);
        pushInstallationService.register(oldSession, request(INSTALLATION_ID));
        pushInstallationService.register(newSession, request("fid-phone-b-old"));

        pushInstallationService.register(newSession, request(INSTALLATION_ID));

        assertThat(pushRepository.findBySesionSid(oldSession.sid())).isEmpty();
        DispositivoPushEntity reassociated = pushRepository.findBySesionSid(newSession.sid()).orElseThrow();
        assertThat(reassociated.getInstallationId()).isEqualTo(INSTALLATION_ID);
        assertThat(reassociated.isActivo()).isTrue();
        assertThat(pushCountByLogin(fixture.login())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE installation_id = ?",
                Integer.class, "fid-phone-b-old")).isZero();
    }

    @Test
    void simultaneousClaimsForOneInstallationSerializeWithoutDuplicateRows() throws Exception {
        Fixture fixture = createUser();
        AuthenticatedUser first = login(fixture, "phone-a", ClientType.MOBILE);
        AuthenticatedUser second = login(fixture, "phone-b", ClientType.MOBILE);
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Void> firstRequest = CompletableFuture.runAsync(() -> {
            await(start);
            pushInstallationService.register(first, request(INSTALLATION_ID));
        });
        CompletableFuture<Void> secondRequest = CompletableFuture.runAsync(() -> {
            await(start);
            pushInstallationService.register(second, request(INSTALLATION_ID));
        });

        start.countDown();
        CompletableFuture.allOf(firstRequest, secondRequest).get(20, TimeUnit.SECONDS);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE installation_id = ?",
                Integer.class, INSTALLATION_ID)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dispositivos_push d
                JOIN sesiones_usuario s ON s.sid = d.sid
                WHERE s.login = ? AND d.activo = true
                """, Integer.class, fixture.login())).isEqualTo(1);
    }

    @Test
    void endpointRequiresMobileAndDoesNotUseIdentityFieldsFromBody() throws Exception {
        Fixture mobileFixture = createUser();
        AuthResult mobile = loginResult(mobileFixture, "phone-mobile", ClientType.MOBILE);
        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mobile))
                        .contentType("application/json")
                        .content("""
                                {"installationId":"api-fid","platform":"ANDROID",
                                 "login":"spoofed.login","sid":"00000000-0000-0000-0000-000000000001",
                                 "deviceId":"spoofed-device"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(pushRepository.findBySesionSid(mobile.response().sid()).orElseThrow().getInstallationId())
                .isEqualTo("api-fid");

        AuthResult sessionWithoutDestination = loginResult(mobileFixture, "phone-without-destination", ClientType.MOBILE);
        mockMvc.perform(delete("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(sessionWithoutDestination)))
                .andExpect(status().isNoContent());

        Fixture webFixture = createUser();
        AuthResult web = loginResult(webFixture, "browser", ClientType.WEB);
        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(web))
                        .contentType("application/json")
                        .content("{\"installationId\":\"web-fid\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_REQUEST.name()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE installation_id = 'web-fid'",
                Integer.class)).isZero();

        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .contentType("application/json")
                        .content("{\"installationId\":\"unauthenticated\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestValidationRejectsBlankOversizedAndUnsupportedPlatformWithoutEchoingInstallationId()
            throws Exception {
        Fixture fixture = createUser();
        AuthResult mobile = loginResult(fixture, "phone", ClientType.MOBILE);
        String oversized = "x".repeat(129);

        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mobile))
                        .contentType("application/json")
                        .content("{\"installationId\":\"   \",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mobile))
                        .contentType("application/json")
                        .content("{\"installationId\":\"" + oversized + "\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(oversized))));
        mockMvc.perform(put("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mobile))
                        .contentType("application/json")
                        .content("{\"installationId\":\"fid\",\"platform\":\"IOS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsExpiredRevokedAndMismatchedSessionsAndInactiveAccounts() {
        Fixture revokedFixture = createUser();
        AuthenticatedUser revoked = login(revokedFixture, "phone-revoked", ClientType.MOBILE);
        sessionService.logout(revoked);
        assertThatThrownBy(() -> pushInstallationService.register(revoked, request("revoked-fid")))
                .isInstanceOf(RevokedSessionException.class);

        Fixture expiredFixture = createUser();
        AuthenticatedUser expired = login(expiredFixture, "phone-expired", ClientType.MOBILE);
        jdbcTemplate.update("UPDATE sesiones_usuario SET fecha_expiracion = ? WHERE sid = ?",
                LocalDateTime.now().minusMinutes(1), expired.sid());
        assertThatThrownBy(() -> pushInstallationService.register(expired, request("expired-fid")))
                .isInstanceOf(ExpiredSessionException.class);

        Fixture mismatchFixture = createUser();
        AuthenticatedUser valid = login(mismatchFixture, "phone-mismatch", ClientType.MOBILE);
        AuthenticatedUser spoofedPrincipal = new AuthenticatedUser("another.login", valid.sid());
        assertThatThrownBy(() -> pushInstallationService.register(spoofedPrincipal, request("mismatch-fid")))
                .isInstanceOf(InvalidJwtException.class);

        Fixture inactiveFixture = createUser();
        AuthenticatedUser inactive = login(inactiveFixture, "phone-inactive", ClientType.MOBILE);
        usuarioService.deactivate(inactive.login());
        assertThatThrownBy(() -> pushInstallationService.register(inactive, request("inactive-fid")))
                .isInstanceOf(RevokedSessionException.class);
    }

    @Test
    void deleteLogoutLogoutAllAndSessionRevokeDeactivateOnlyTheirSessionDestinations() throws Exception {
        Fixture fixture = createUser();
        AuthResult first = loginResult(fixture, "phone-a", ClientType.MOBILE);
        AuthResult second = loginResult(fixture, "phone-b", ClientType.MOBILE);
        pushInstallationService.register(user(first), request("fid-a"));
        pushInstallationService.register(user(second), request("fid-b"));

        mockMvc.perform(delete("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first)))
                .andExpect(status().isNoContent());
        assertThat(pushRepository.findBySesionSid(first.response().sid()).orElseThrow().isActivo()).isFalse();
        assertThat(pushRepository.findBySesionSid(second.response().sid()).orElseThrow().isActivo()).isTrue();
        mockMvc.perform(delete("/api/v1/mobile/push-installation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first)))
                .andExpect(status().isNoContent());

        pushInstallationService.register(user(first), request("fid-a"));
        sessionService.revoke(user(second), second.response().sid());
        assertThat(pushRepository.findBySesionSid(second.response().sid()).orElseThrow().isActivo()).isFalse();
        assertThat(pushRepository.findBySesionSid(first.response().sid()).orElseThrow().isActivo()).isTrue();

        sessionService.logoutAll(user(first));
        assertThat(pushRepository.findBySesionSid(first.response().sid()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    void sessionReplacementRefreshReusePasswordAndAccountDeactivationInvalidateDestinations() {
        Fixture replacementFixture = createUser();
        AuthenticatedUser old = login(replacementFixture, "same-device", ClientType.MOBILE);
        pushInstallationService.register(old, request("replace-fid"));
        AuthenticatedUser replacement = login(replacementFixture, "same-device", ClientType.MOBILE);
        assertThat(pushRepository.findBySesionSid(old.sid()).orElseThrow().isActivo()).isFalse();
        pushInstallationService.register(replacement, request("replace-fid"));
        assertThat(pushRepository.findBySesionSid(replacement.sid()).orElseThrow().isActivo()).isTrue();

        Fixture refreshFixture = createUser();
        AuthResult refresh = loginResult(refreshFixture, "refresh-device", ClientType.MOBILE);
        pushInstallationService.register(user(refresh), request("refresh-fid"));
        authService.refresh(refresh.refreshToken(), ClientType.MOBILE);
        assertThatThrownBy(() -> authService.refresh(refresh.refreshToken(), ClientType.MOBILE))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(pushRepository.findBySesionSid(refresh.response().sid()).orElseThrow().isActivo()).isFalse();

        Fixture passwordFixture = createUser();
        AuthenticatedUser password = login(passwordFixture, "password-device", ClientType.MOBILE);
        pushInstallationService.register(password, request("password-fid"));
        usuarioService.changePassword(password.login(), new ChangePasswordRequest("nueva-clave-ficticia"));
        assertThat(pushRepository.findBySesionSid(password.sid()).orElseThrow().isActivo()).isFalse();

        Fixture disabledUserFixture = createUser();
        AuthenticatedUser disabledUser = login(disabledUserFixture, "disabled-user-device", ClientType.MOBILE);
        pushInstallationService.register(disabledUser, request("disabled-user-fid"));
        usuarioService.deactivate(disabledUser.login());
        assertThat(pushRepository.findBySesionSid(disabledUser.sid()).orElseThrow().isActivo()).isFalse();

        Fixture disabledPersonFixture = createUser();
        AuthenticatedUser disabledPerson = login(disabledPersonFixture, "disabled-person-device", ClientType.MOBILE);
        pushInstallationService.register(disabledPerson, request("disabled-person-fid"));
        personaService.deactivate(disabledPersonFixture.codper());
        assertThat(pushRepository.findBySesionSid(disabledPerson.sid()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    void schemaUniqueInstallationAndSessionConstraintsAreEnforcedByDatabase() {
        Fixture fixture = createUser();
        AuthenticatedUser first = login(fixture, "phone-a", ClientType.MOBILE);
        AuthenticatedUser second = login(fixture, "phone-b", ClientType.MOBILE);
        pushInstallationService.register(first, request(INSTALLATION_ID));
        pushInstallationService.register(second, request(INSTALLATION_ID));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE installation_id = ?",
                Integer.class, INSTALLATION_ID)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT sid FROM dispositivos_push WHERE installation_id = ?",
                UUID.class, INSTALLATION_ID)).isEqualTo(second.sid());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispositivos_push WHERE sid = ?",
                Integer.class, first.sid())).isZero();
    }

    @Test
    void databaseRejectsDuplicateSessionInstallationAndUnsupportedPlatform() {
        Fixture fixture = createUser();
        AuthenticatedUser first = login(fixture, "phone-a", ClientType.MOBILE);
        AuthenticatedUser second = login(fixture, "phone-b", ClientType.MOBILE);
        pushInstallationService.register(first, request("constraint-fid"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO dispositivos_push (sid, installation_id, platform)
                VALUES (?, ?, 'ANDROID')
                """, second.sid(), "constraint-fid"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO dispositivos_push (sid, installation_id, platform)
                VALUES (?, ?, 'ANDROID')
                """, first.sid(), "another-fid"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE dispositivos_push SET platform = 'IOS' WHERE sid = ?", first.sid()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void eligibleDestinationsRequireMobileLiveSessionAndActiveUserAndPersonAcrossDevices() {
        Fixture activeFixture = createUser();
        AuthenticatedUser firstMobile = login(activeFixture, "active-phone-a", ClientType.MOBILE);
        AuthenticatedUser secondMobile = login(activeFixture, "active-phone-b", ClientType.MOBILE);
        pushInstallationService.register(firstMobile, request("eligible-fid-a"));
        pushInstallationService.register(secondMobile, request("eligible-fid-b"));

        AuthenticatedUser web = login(activeFixture, "active-browser", ClientType.WEB);
        jdbcTemplate.update("INSERT INTO dispositivos_push (sid, installation_id, platform) VALUES (?, ?, 'ANDROID')",
                web.sid(), "web-session-fid");

        AuthenticatedUser revoked = login(activeFixture, "revoked-phone", ClientType.MOBILE);
        pushInstallationService.register(revoked, request("revoked-session-fid"));
        sessionService.logout(revoked);
        jdbcTemplate.update("UPDATE dispositivos_push SET activo = true WHERE sid = ?", revoked.sid());

        AuthenticatedUser expired = login(activeFixture, "expired-phone", ClientType.MOBILE);
        pushInstallationService.register(expired, request("expired-session-fid"));
        jdbcTemplate.update("UPDATE sesiones_usuario SET fecha_expiracion = ? WHERE sid = ?",
                LocalDateTime.ofInstant(java.time.Clock.systemUTC().instant(), ZoneOffset.UTC).minusMinutes(1),
                expired.sid());
        jdbcTemplate.update("UPDATE dispositivos_push SET activo = true WHERE sid = ?", expired.sid());

        List<PushDestination> eligible = pushDestinationService.findEligible(activeFixture.login());
        assertThat(eligible).extracting(PushDestination::installationId)
                .containsExactly("eligible-fid-a", "eligible-fid-b");
        assertThat(pushDestinationService.findEligible("different.login")).isEmpty();

        Fixture inactiveUserFixture = createUser();
        AuthenticatedUser inactiveUser = login(inactiveUserFixture, "inactive-user-phone", ClientType.MOBILE);
        pushInstallationService.register(inactiveUser, request("inactive-user-fid"));
        jdbcTemplate.update("UPDATE usuarios SET estado = 0 WHERE login = ?", inactiveUserFixture.login());
        assertThat(pushDestinationService.findEligible(inactiveUserFixture.login())).isEmpty();

        Fixture inactivePersonFixture = createUser();
        AuthenticatedUser inactivePerson = login(inactivePersonFixture, "inactive-person-phone", ClientType.MOBILE);
        pushInstallationService.register(inactivePerson, request("inactive-person-fid"));
        jdbcTemplate.update("UPDATE personas SET estado = 0 WHERE codper = ?", inactivePersonFixture.codper());
        assertThat(pushDestinationService.findEligible(inactivePersonFixture.login())).isEmpty();
    }

    private Fixture createUser() {
        String login = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Persona persona = new Persona();
        persona.setCi("P" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        persona.setNombre("Persona Push");
        persona.setGenero('F');
        persona.setCorreo("push@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('I');
        persona.setEstado((short) 1);
        Persona savedPersona = personaRepository.saveAndFlush(persona);

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPasswd(passwordEncoder.encode(PASSWORD));
        usuario.setEstado((short) 1);
        usuario.setPersona(savedPersona);
        usuarioRepository.saveAndFlush(usuario);

        Fixture fixture = new Fixture(login, savedPersona.getCodper());
        fixtures.add(fixture);
        return fixture;
    }

    private AuthenticatedUser login(Fixture fixture, String deviceId, ClientType clientType) {
        return user(loginResult(fixture, deviceId, clientType));
    }

    private AuthResult loginResult(Fixture fixture, String deviceId, ClientType clientType) {
        return authService.login(new LoginRequest(fixture.login(), PASSWORD, deviceId, "Android", clientType));
    }

    private AuthenticatedUser user(AuthResult result) {
        return new AuthenticatedUser(result.response().login(), result.response().sid());
    }

    private String bearer(AuthResult result) {
        return "Bearer " + result.response().accessToken();
    }

    private PushInstallationRequest request(String installationId) {
        return new PushInstallationRequest(installationId, PushPlatform.ANDROID);
    }

    private int pushCountByLogin(String login) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dispositivos_push d
                JOIN sesiones_usuario s ON s.sid = d.sid
                WHERE s.login = ?
                """, Integer.class, login);
    }

    private void await(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("No se inició la prueba concurrente.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La prueba concurrente fue interrumpida.", exception);
        }
    }

    private record Fixture(String login, Integer codper) {
    }
}
