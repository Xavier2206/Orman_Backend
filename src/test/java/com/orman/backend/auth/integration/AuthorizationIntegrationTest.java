package com.orman.backend.auth.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.UserAuthorityService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolUsuId;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@Import(AuthorizationIntegrationTest.AuthorizationTestConfig.class)
class AuthorizationIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private UserAuthorityService userAuthorityService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private RolService rolService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private RolRepository rolRepository;
    @Autowired private RolUsuRepository rolUsuRepository;
    @Autowired private SesionUsuarioRepository sesionRepository;

    @Test
    void loadsMainActiveRolesIntoAuthenticationAndKeepsJwtMinimal() throws Exception {
        UsuarioResponse usuario = createUsuario("AUTHZ-111-A", "authz.roles.main");
        AuthResult login = login(usuario.login(), "device-main");
        String accessToken = login.response().accessToken();

        expectAuthorities(accessToken);

        Rol propietario = activeRole("PROPIETARIO");
        Rol administrador = activeRole("ADMINISTRADOR");
        Rol inquilino = activeRole("INQUILINO");
        rolUsuService.assign(usuario.login(), propietario.getCodr());
        rolUsuService.assign(usuario.login(), administrador.getCodr());
        rolUsuService.assign(usuario.login(), inquilino.getCodr());

        expectAuthorities(accessToken, "ROLE_ADMINISTRADOR", "ROLE_INQUILINO", "ROLE_PROPIETARIO");
        assertThat(userAuthorityService.loadAuthorities(usuario.login()))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR", "ROLE_INQUILINO", "ROLE_PROPIETARIO");
        assertThat(SignedJWT.parse(accessToken).getJWTClaimsSet().getClaims().keySet())
                .containsExactlyInAnyOrder("sub", "sid", "iss", "iat", "exp");
        assertSessionRemainsActive(login.response().sid());
    }

    @Test
    void reflectsAssignmentAndRoleStateChangesWithSameSessionAndAccessToken() throws Exception {
        UsuarioResponse usuario = createUsuario("AUTHZ-111-B", "authz.roles.dynamic");
        AuthResult login = login(usuario.login(), "device-dynamic");
        String accessToken = login.response().accessToken();
        Rol administrador = activeRole("ADMINISTRADOR");

        expectAdminDenied(accessToken);

        rolUsuService.assign(usuario.login(), administrador.getCodr());
        expectAdminAllowed(accessToken);

        rolUsuService.remove(usuario.login(), administrador.getCodr());
        expectAdminDenied(accessToken);

        rolUsuService.assign(usuario.login(), administrador.getCodr());
        expectAdminAllowed(accessToken);

        rolService.deactivate(administrador.getCodr());
        rolRepository.flush();
        assertThat(rolUsuRepository.existsById(new RolUsuId(usuario.login(), administrador.getCodr()))).isTrue();
        expectAdminDenied(accessToken);

        rolService.activate(administrador.getCodr());
        rolRepository.flush();
        expectAdminAllowed(accessToken);

        assertThat(login.response().accessToken()).isEqualTo(accessToken);
        assertSessionRemainsActive(login.response().sid());
    }

    @Test
    void ignoresInactiveEmptyAndNormalizedDuplicateRoleNames() throws Exception {
        UsuarioResponse usuario = createUsuario("AUTHZ-111-C", "authz.roles.edge");
        AuthResult login = login(usuario.login(), "device-edge");

        Rol firstDuplicate = persistRole("AUTHZ_DUPLICADO_111", (short) 1);
        Rol secondDuplicate = persistRole(" authz_duplicado_111 ", (short) 1);
        Rol blank = persistRole("   ", (short) 1);
        Rol emptyPrefix = persistRole("ROLE_", (short) 1);
        Rol inactive = persistRole("AUTHZ_INACTIVO_111", (short) 1);

        rolUsuService.assign(usuario.login(), firstDuplicate.getCodr());
        rolUsuService.assign(usuario.login(), secondDuplicate.getCodr());
        rolUsuService.assign(usuario.login(), blank.getCodr());
        rolUsuService.assign(usuario.login(), emptyPrefix.getCodr());
        rolUsuService.assign(usuario.login(), inactive.getCodr());
        inactive.setEstado((short) 0);
        rolRepository.saveAndFlush(inactive);

        expectAuthorities(login.response().accessToken(), "ROLE_AUTHZ_DUPLICADO_111");
        assertSessionRemainsActive(login.response().sid());
    }

    @Test
    void keepsPublicLoginAndUses401Before403() throws Exception {
        UsuarioResponse usuario = createUsuario("AUTHZ-111-D", "authz.roles.http");

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"authz.roles.http","password":"clave-ficticia",
                                 "deviceId":"device-http","deviceName":"Integration","clientType":"MOBILE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(usuario.login()))
                .andReturn().getResponse().getContentAsString();
        JsonNode loginJson = objectMapper.readTree(response);

        mockMvc.perform(get("/test/authorization/owner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        String accessToken = loginJson.get("accessToken").asText();
        expectOwnerDenied(accessToken);
    }

    private void expectAuthorities(String accessToken, String... authorities) throws Exception {
        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(get("/test/authorization/authorities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(authorities.length));
        for (int index = 0; index < authorities.length; index++) {
            result.andExpect(jsonPath("$[" + index + "]").value(authorities[index]));
        }
    }

    private void expectOwnerAllowed(String accessToken) throws Exception {
        mockMvc.perform(get("/test/authorization/owner")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));
    }

    private void expectOwnerDenied(String accessToken) throws Exception {
        String body = mockMvc.perform(get("/test/authorization/owner")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.detail")
                        .value("No tiene autorización para realizar esta operación."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain("role_propietario", "accesstoken", "refreshtoken",
                "password", "passwd", "hash", "sql", "stacktrace");
    }

    private void expectAdminAllowed(String accessToken) throws Exception {
        mockMvc.perform(get("/test/authorization/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));
    }

    private void expectAdminDenied(String accessToken) throws Exception {
        mockMvc.perform(get("/test/authorization/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    private UsuarioResponse createUsuario(String ci, String login) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona autorización", null,
                null, "F", null, "persona.authorization@example.test", "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, null, persona.codper()));
    }

    private AuthResult login(String login, String deviceId) {
        return authService.login(new LoginRequest(login, PASSWORD, deviceId, "Integration", ClientType.MOBILE));
    }

    private Rol activeRole(String name) {
        Rol rol = rolRepository.findAll().stream()
                .filter(candidate -> name.equals(candidate.getNombre()))
                .findFirst()
                .orElseGet(() -> persistRole(name, (short) 1));
        rol.setEstado((short) 1);
        return rolRepository.saveAndFlush(rol);
    }

    private Rol persistRole(String name, short state) {
        Rol rol = new Rol();
        rol.setNombre(name);
        rol.setEstado(state);
        return rolRepository.saveAndFlush(rol);
    }

    private void assertSessionRemainsActive(java.util.UUID sid) {
        SesionUsuario session = sesionRepository.findById(sid).orElseThrow();
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.getFechaRevocacion()).isNull();
        assertThat(session.getMotivoRevocacion()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuthorizationTestConfig {

        @Bean
        AuthorizationProbeService authorizationProbeService() {
            return new AuthorizationProbeService();
        }

        @Bean
        AuthorizationProbeController authorizationProbeController(AuthorizationProbeService service) {
            return new AuthorizationProbeController(service);
        }
    }

    static class AuthorizationProbeService {

        @PreAuthorize("hasRole('PROPIETARIO')")
        public String ownerOnly() {
            return "allowed";
        }

        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public String adminOnly() {
            return "allowed";
        }
    }

    @RestController
    @RequestMapping("/test/authorization")
    static class AuthorizationProbeController {

        private final AuthorizationProbeService service;

        AuthorizationProbeController(AuthorizationProbeService service) {
            this.service = service;
        }

        @GetMapping("/authorities")
        List<String> authorities(Authentication authentication) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        }

        @GetMapping("/owner")
        String owner() {
            return service.ownerOnly();
        }

        @GetMapping("/admin")
        String admin() {
            return service.adminOnly();
        }
    }
}
