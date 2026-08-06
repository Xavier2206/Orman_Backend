package com.orman.backend.auth.integration;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.impl.NimbusJwtService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class SecurityHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private SesionUsuarioRepository sessionRepository;
    @Autowired private JwtProperties jwtProperties;

    @Test
    void protectsEndpointsAndAuthenticatesValidBearerWithoutAuthorities() throws Exception {
        AuthResult login = createAndLogin("SEC-201", "security.valid", "device-1", ClientType.MOBILE);

        mockMvc.perform(get("/api/v1/auth/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        String body = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sid").value(login.response().sid().toString()))
                .andExpect(jsonPath("$[0].current").value(true))
                .andReturn().getResponse().getContentAsString().toLowerCase();

        assertThat(body).doesNotContain("password", "passwd", "refreshtoken", "refreshtokenhash",
                "jwt_secret", "roles", "permisos");
    }

    @Test
    void rejectsTamperedExpiredAndWrongIssuerTokensWithProblemDetail() throws Exception {
        AuthResult login = createAndLogin("SEC-202", "security.jwt", "device-1", ClientType.MOBILE);
        String token = login.response().accessToken();
        int signatureStart = token.lastIndexOf('.') + 1;
        char firstSignatureCharacter = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (firstSignatureCharacter == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);
        expectUnauthorized(tampered, "INVALID_TOKEN");

        JwtProperties wrongIssuerProperties = new JwtProperties(jwtProperties.secret(), "different-issuer",
                jwtProperties.accessTokenExpirationMinutes(), jwtProperties.refreshTokenExpirationDays());
        String wrongIssuer = new NimbusJwtService(wrongIssuerProperties, Clock.systemUTC())
                .generateAccessToken(login.response().login(), login.response().sid());
        expectUnauthorized(wrongIssuer, "INVALID_TOKEN");

        Clock oldClock = Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC);
        String expired = new NimbusJwtService(jwtProperties, oldClock)
                .generateAccessToken(login.response().login(), login.response().sid());
        expectUnauthorized(expired, "TOKEN_EXPIRED");
    }

    @Test
    void rejectsRevokedExpiredAndInactiveSessions() throws Exception {
        AuthResult revokedLogin = createAndLogin("SEC-203-A", "security.revoked", "device-1", ClientType.MOBILE);
        SesionUsuario revoked = sessionRepository.findById(revokedLogin.response().sid()).orElseThrow();
        revoked.revoke(java.time.LocalDateTime.now(ZoneOffset.UTC), RevocationReason.ADMIN_REVOKED);
        sessionRepository.saveAndFlush(revoked);
        expectUnauthorized(revokedLogin.response().accessToken(), "SESSION_REVOKED");

        AuthResult expiredLogin = createAndLogin("SEC-203-B", "security.expired", "device-1", ClientType.MOBILE);
        SesionUsuario expired = sessionRepository.findById(expiredLogin.response().sid()).orElseThrow();
        ReflectionTestUtils.setField(expired, "fechaExpiracion", java.time.LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        sessionRepository.saveAndFlush(expired);
        expectUnauthorized(expiredLogin.response().accessToken(), "SESSION_EXPIRED");

        AuthResult userLogin = createAndLogin("SEC-203-C", "security.user-off", "device-1", ClientType.MOBILE);
        Usuario user = usuarioRepository.findById(userLogin.response().login()).orElseThrow();
        user.setEstado((short) 0);
        usuarioRepository.saveAndFlush(user);
        expectUnauthorized(userLogin.response().accessToken(), "INVALID_TOKEN");

        AuthResult personLogin = createAndLogin("SEC-203-D", "security.person-off", "device-1", ClientType.MOBILE);
        Usuario personUser = usuarioRepository.findById(personLogin.response().login()).orElseThrow();
        Persona person = personUser.getPersona();
        person.setEstado((short) 0);
        personaRepository.saveAndFlush(person);
        expectUnauthorized(personLogin.response().accessToken(), "INVALID_TOKEN");
    }

    @Test
    void logsOutListsAndRevokesOnlyOwnedSessions() throws Exception {
        createUser("SEC-204-A", "session.owner");
        AuthResult current = login("session.owner", "device-current", ClientType.MOBILE);
        AuthResult otherOwn = login("session.owner", "device-other", ClientType.MOBILE);
        AuthResult foreign = createAndLogin("SEC-204-B", "session.foreign", "device-foreign", ClientType.MOBILE);

        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.sid == '%s')]", foreign.response().sid()).doesNotExist());

        mockMvc.perform(delete("/api/v1/auth/sessions/{sid}", java.util.UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/auth/sessions/{sid}", foreign.response().sid())
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        assertThat(sessionRepository.findById(foreign.response().sid()).orElseThrow().isRevoked()).isFalse();

        mockMvc.perform(delete("/api/v1/auth/sessions/{sid}", otherOwn.response().sid())
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent());
        assertThat(sessionRepository.findById(otherOwn.response().sid()).orElseThrow().getMotivoRevocacion())
                .isEqualTo(RevocationReason.ADMIN_REVOKED);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        assertThat(sessionRepository.findById(current.response().sid()).orElseThrow().getMotivoRevocacion())
                .isEqualTo(RevocationReason.LOGOUT);

        AuthResult first = login("session.owner", "device-all-1", ClientType.MOBILE);
        AuthResult second = login("session.owner", "device-all-2", ClientType.MOBILE);
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(first)))
                .andExpect(status().isNoContent());
        assertThat(sessionRepository.findById(first.response().sid()).orElseThrow().getMotivoRevocacion())
                .isEqualTo(RevocationReason.LOGOUT_ALL);
        assertThat(sessionRepository.findById(second.response().sid()).orElseThrow().getMotivoRevocacion())
                .isEqualTo(RevocationReason.LOGOUT_ALL);
    }

    @Test
    void appliesCorsAllowlistAndCsrfOnlyToCookieRefresh() throws Exception {
        AuthResult mobile = createAndLogin("SEC-205-A", "cors.mobile", "mobile", ClientType.MOBILE);
        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        createUser("SEC-205-B", "csrf.web");
        org.springframework.test.web.servlet.MvcResult webLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"login":"csrf.web","password":"clave-ficticia","deviceId":"browser-http",
                                 "deviceName":"Chrome Test","clientType":"WEB"}
                                """))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .exists("XSRF-TOKEN"))
                .andExpect(header().exists("X-XSRF-TOKEN"))
                .andReturn();
        jakarta.servlet.http.Cookie refreshCookie = webLogin.getResponse().getCookie("orman_refresh");
        jakarta.servlet.http.Cookie csrfCookie = webLogin.getResponse().getCookie("XSRF-TOKEN");
        String csrfHeader = webLogin.getResponse().getHeader("X-XSRF-TOKEN");
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie, csrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfHeader))
                .andExpect(status().isOk());
    }

    private void expectUnauthorized(String token, String errorCode) throws Exception {
        mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value(errorCode))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.instance").value("/api/v1/auth/sessions"));
    }

    private AuthResult createAndLogin(String ci, String login, String deviceId, ClientType type) {
        createUser(ci, login);
        return login(login, deviceId, type);
    }

    private void createUser(String ci, String login) {
        PersonaResponse person = personaService.create(new CreatePersonaRequest(ci, "Persona seguridad", null,
                null, "F", null, null, "70000000", "A", null));
        usuarioService.create(new CreateUsuarioRequest(login, "clave-ficticia", null, person.codper()));
    }

    private AuthResult login(String login, String deviceId, ClientType type) {
        return authService.login(new LoginRequest(login, "clave-ficticia", deviceId, "Test device", type));
    }

    private String bearer(AuthResult result) {
        return "Bearer " + result.response().accessToken();
    }
}
