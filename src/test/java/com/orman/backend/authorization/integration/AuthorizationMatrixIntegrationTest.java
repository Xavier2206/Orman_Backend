package com.orman.backend.authorization.integration;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolUsuId;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuthorizationMatrixIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private RolRepository rolRepository;
    @Autowired private RolUsuRepository rolUsuRepository;
    @Autowired private SesionUsuarioRepository sesionUsuarioRepository;
    @Autowired private AuthService authService;

    private Rol ownerRole;
    private Rol tenantRole;
    private Fixture owner;
    private Fixture tenant;
    private Fixture common;
    private Fixture withoutRoles;

    @BeforeEach
    void setUp() {
        ownerRole = activeRole("PROPIETARIO");
        tenantRole = activeRole("INQUILINO");
        makeExistingOwnersInactiveForTransactionalIsolation();

        owner = createFixture("M112-OWN", "m112.owner", "owner-device");
        tenant = createFixture("M112-TEN", "m112.tenant", "tenant-device");
        common = createFixture("M112-COM", "m112.common", "common-device");
        withoutRoles = createFixture("M112-NOR", "m112.norole", "norole-device");
        rolUsuService.assign(owner.login(), ownerRole.getCodr());
        rolUsuService.assign(tenant.login(), tenantRole.getCodr());

        owner = owner.withLogin(login(owner.login(), "owner-login"));
        tenant = tenant.withLogin(login(tenant.login(), "tenant-login"));
        common = common.withLogin(login(common.login(), "common-login"));
        withoutRoles = withoutRoles.withLogin(login(withoutRoles.login(), "norole-login"));
    }

    @Test
    void keepsLoginRefreshAndOwnSessionsAvailableWithoutRoles() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(withoutRoles.login(), "public-login")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/sessions").headers(bearer(withoutRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sid").isNotEmpty());

        mockMvc.perform(delete("/api/v1/auth/sessions/{sid}", tenant.auth().response().sid())
                        .headers(bearer(withoutRoles)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/auth/logout").headers(bearer(withoutRoles)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void appliesPersonMatrixAndProtectsOwnerTargetFromTenant() throws Exception {
        mockMvc.perform(get("/api/v1/personas").headers(bearer(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/personas/resumen").headers(bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPersonas").isNumber())
                .andExpect(jsonPath("$.activas").isNumber())
                .andExpect(jsonPath("$.inactivas").isNumber())
                .andExpect(jsonPath("$.conUsuario").isNumber());
        mockMvc.perform(post("/api/v1/personas").headers(bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(personJson("M112-NEW-O")))
                .andExpect(status().isCreated());
        expectForbidden(get("/api/v1/personas/{codper}", owner.person().codper()), tenant);
        expectForbidden(put("/api/v1/personas/{codper}", common.person().codper())
                .contentType(MediaType.APPLICATION_JSON).content(personUpdateJson("M112-COM", "1")), tenant);
        expectForbidden(get("/api/v1/personas"), tenant);
        expectForbidden(get("/api/v1/personas/resumen"), tenant);

        mockMvc.perform(get("/api/v1/personas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
        mockMvc.perform(get("/api/v1/personas/resumen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void protectsUnitStateActionsWithTheOwnerRoleOnly() throws Exception {
        expectForbidden(patch("/api/v1/unidades/{coduni}/activar", Integer.MAX_VALUE), tenant);
        expectForbidden(patch("/api/v1/unidades/{coduni}/desactivar", Integer.MAX_VALUE), tenant);

        mockMvc.perform(patch("/api/v1/unidades/{coduni}/activar", Integer.MAX_VALUE)
                        .headers(bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/unidades/{coduni}/desactivar", Integer.MAX_VALUE)
                        .headers(bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/unidades/{coduni}/desactivar", Integer.MAX_VALUE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void appliesHttpAuthorizationMatrixToPersonaPhotos() throws Exception {
        Fixture otherOwner = createFixture("M112-OWN-PHOTO", "m112.photo.owner", "photo-device");
        rolUsuService.assign(otherOwner.login(), ownerRole.getCodr());
        otherOwner = otherOwner.withLogin(login(otherOwner.login(), "photo-login"));
        var photo = validPhoto();

        mockMvc.perform(multipart("/api/v1/personas/{codper}/foto", owner.person().codper()).file(photo)
                        .with(request -> { request.setMethod("PUT"); return request; }).headers(bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/personas/{codper}/foto", owner.person().codper()).headers(bearer(owner)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        mockMvc.perform(multipart("/api/v1/personas/{codper}/foto", owner.person().codper()).file(validPhoto())
                        .with(request -> { request.setMethod("PUT"); return request; }).headers(bearer(owner)))
                .andExpect(status().isNoContent());

        expectForbidden(get("/api/v1/personas/{codper}/foto", owner.person().codper()), tenant);
        expectForbidden(get("/api/v1/personas/{codper}/foto", otherOwner.person().codper()), owner);
        expectForbidden(multipart("/api/v1/personas/{codper}/foto", otherOwner.person().codper()).file(validPhoto())
                .with(request -> { request.setMethod("PUT"); return request; }), owner);
        expectForbidden(delete("/api/v1/personas/{codper}/foto", otherOwner.person().codper()), owner);
        expectForbidden(get("/api/v1/personas/{codper}/foto", common.person().codper()), owner);
        expectForbidden(get("/api/v1/personas/{codper}/foto", owner.person().codper()), withoutRoles);
        mockMvc.perform(get("/api/v1/personas/{codper}/foto", owner.person().codper()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/personas/{codper}/foto", owner.person().codper()).headers(bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/personas/{codper}/foto", owner.person().codper()).headers(bearer(owner)))
                .andExpect(status().isNotFound());
    }

    private org.springframework.mock.web.MockMultipartFile validPhoto() throws Exception {
        var image = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", bytes);
        return new org.springframework.mock.web.MockMultipartFile("foto", "foto.jpg", "image/jpeg", bytes.toByteArray());
    }

    @Test
    void appliesUserMatrixAndBlocksTenantManagement() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").headers(bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.login == 'm112.owner')]").isNotEmpty());

        expectForbidden(get("/api/v1/usuarios/{login}", owner.login()), tenant);
        expectForbidden(patch("/api/v1/usuarios/{login}/desactivar", common.login()), tenant);
        expectForbidden(get("/api/v1/usuarios"), tenant);
    }

    @Test
    void allowsOnlySelfOrOwnerToChangePasswordAndKeepsSessionRevocation() throws Exception {
        expectForbidden(put("/api/v1/usuarios/{login}/password", owner.login())
                .contentType(MediaType.APPLICATION_JSON).content(passwordJson()), tenant);

        mockMvc.perform(put("/api/v1/usuarios/{login}/password", tenant.login())
                        .headers(bearer(tenant)).contentType(MediaType.APPLICATION_JSON).content(passwordJson()))
                .andExpect(status().isNoContent());
        assertThat(sesionUsuarioRepository.findById(tenant.auth().response().sid()).orElseThrow().isRevoked())
                .isTrue();

        mockMvc.perform(put("/api/v1/usuarios/{login}/password", withoutRoles.login())
                        .headers(bearer(withoutRoles)).contentType(MediaType.APPLICATION_JSON).content(passwordJson()))
                .andExpect(status().isNoContent());
        assertThat(sesionUsuarioRepository.findById(withoutRoles.auth().response().sid()).orElseThrow().isRevoked())
                .isTrue();

        mockMvc.perform(put("/api/v1/usuarios/{login}/password", common.login())
                        .headers(bearer(owner)).contentType(MediaType.APPLICATION_JSON).content(passwordJson()))
                .andExpect(status().isNoContent());
        assertThat(sesionUsuarioRepository.findById(common.auth().response().sid()).orElseThrow().isRevoked())
                .isTrue();
    }

    @Test
    void restrictsFixedRolesAndReflectsOwnerDelegationWithSameJwt() throws Exception {
        expectForbidden(get("/api/v1/roles"), tenant);
        expectForbidden(get("/api/v1/roles/resumen"), tenant);
        mockMvc.perform(get("/api/v1/roles").headers(bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/roles/resumen").headers(bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRoles").value(2));
        mockMvc.perform(post("/api/v1/roles").headers(bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"OTRO\"}"))
                .andExpect(status().isMethodNotAllowed());
        expectLastOwner(delete("/api/v1/usuarios/{login}/roles/{codr}", owner.login(), ownerRole.getCodr()));
        expectLastOwner(patch("/api/v1/usuarios/{login}/desactivar", owner.login()));
        expectLastOwner(patch("/api/v1/personas/{codper}/desactivar", owner.person().codper()));
        expectLastOwner(delete("/api/v1/personas/{codper}", owner.person().codper()));
        expectLastOwner(put("/api/v1/usuarios/{login}", owner.login())
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":0}"));
        expectLastOwner(put("/api/v1/personas/{codper}", owner.person().codper())
                .contentType(MediaType.APPLICATION_JSON).content(personUpdateJson("M112-OWN", "0")));

        assertThat(rolUsuRepository.existsById(new RolUsuId(owner.login(), ownerRole.getCodr()))).isTrue();
        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(1);
    }

    @Test
    void allowsOwnerToRemoveOneOfTwoOwners() throws Exception {
        Fixture secondOwner = createFixture("M112-OWN2", "m112.owner2", "owner2-device");
        rolUsuService.assign(secondOwner.login(), ownerRole.getCodr());
        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(2);

        mockMvc.perform(patch("/api/v1/usuarios/{login}/desactivar", secondOwner.login())
                        .headers(bearer(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/usuarios/{login}/roles/{codr}", secondOwner.login(), ownerRole.getCodr())
                        .headers(bearer(owner)))
                .andExpect(status().isNoContent());

        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(1);
        assertThat(rolUsuRepository.existsById(new RolUsuId(owner.login(), ownerRole.getCodr()))).isTrue();
    }

    private void expectForbidden(org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?> request,
            Fixture actor) throws Exception {
        mockMvc.perform(request.headers(bearer(actor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ROLE_PROPIETARIO"))));
    }

    private void expectLastOwner(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request.headers(bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LAST_OWNER_REQUIRED"))
                .andExpect(jsonPath("$.detail").value("Debe permanecer al menos un propietario activo."));
    }

    private HttpHeaders bearer(Fixture fixture) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(fixture.auth().response().accessToken());
        return headers;
    }

    private Fixture createFixture(String ci, String login, String device) {
        PersonaResponse person = personaService.create(new CreatePersonaRequest(ci, "Persona matriz", null, null,
                "F", null, "persona.matrix@example.test", "70000000", "A", null));
        UsuarioResponse user = usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, null, person.codper()));
        return new Fixture(person, user, null, device);
    }

    private AuthResult login(String login, String device) {
        return authService.login(new LoginRequest(login, PASSWORD, device, "Integration", ClientType.MOBILE));
    }

    private Rol activeRole(String name) {
        Rol rol = rolRepository.findAll().stream().filter(candidate -> name.equals(candidate.getNombre()))
                .findFirst().orElseGet(() -> {
                    Rol created = new Rol();
                    created.setNombre(name);
                    created.setEstado((short) 1);
                    return rolRepository.saveAndFlush(created);
                });
        rol.setEstado((short) 1);
        return rolRepository.saveAndFlush(rol);
    }

    private void makeExistingOwnersInactiveForTransactionalIsolation() {
        rolUsuRepository.findByIdCodrOrderByFechaAsignacionAsc(ownerRole.getCodr()).forEach(assignment -> {
            assignment.getUsuario().setEstado((short) 0);
        });
        rolUsuRepository.flush();
    }

    private String loginJson(String login, String deviceId) {
        return "{\"login\":\"" + login + "\",\"password\":\"" + PASSWORD
                + "\",\"deviceId\":\"" + deviceId
                + "\",\"deviceName\":\"Integration\",\"clientType\":\"MOBILE\"}";
    }

    private String personJson(String ci) {
        return "{\"ci\":\"" + ci + "\",\"nombre\":\"Persona nueva\",\"genero\":\"F\","
                + "\"correo\":\"persona.matriz@example.test\",\"telefono\":\"70000000\",\"tipoPersona\":\"A\"}";
    }

    private String personUpdateJson(String ci, String state) {
        return "{\"ci\":\"" + ci + "\",\"nombre\":\"Persona actualizada\",\"genero\":\"F\","
                + "\"estado\":\"" + state + "\",\"correo\":\"persona.matriz@example.test\",\"telefono\":\"70000000\",\"tipoPersona\":\"A\"}";
    }

    private String passwordJson() {
        return "{\"newPassword\":\"nueva-clave-ficticia\"}";
    }

    private record Fixture(PersonaResponse person, UsuarioResponse user, AuthResult auth, String device) {
        String login() {
            return user.login();
        }

        Fixture withLogin(AuthResult login) {
            return new Fixture(person, user, login, device);
        }
    }
}
