package com.orman.backend.auth.integration;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.menu.entity.MePro;
import com.orman.backend.menu.entity.MeProId;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.repository.MeProRepository;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.repository.ProcesoRepository;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolMe;
import com.orman.backend.role.entity.RolMeId;
import com.orman.backend.role.entity.RolUsu;
import com.orman.backend.role.repository.RolMeRepository;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AuthContextIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";
    private static final String CONTEXT_URL = "/api/v1/auth/context";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private RolUsuRepository rolUsuRepository;
    @Autowired private RolMeRepository rolMeRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private MeProRepository meProRepository;
    @Autowired private ProcesoRepository procesoRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsUnauthenticatedContextRequests() throws Exception {
        mockMvc.perform(get(CONTEXT_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void returnsOnlyTheCurrentUserAndPreservesActiveNavigationRelationships() throws Exception {
        UsuarioResponse current = createUser("CTX-001", "context.actual", "https://example.test/foto.png");
        UsuarioResponse other = createUser("CTX-002", "context.otro", null);
        Usuario currentEntity = usuarioRepository.findById(current.login()).orElseThrow();

        Rol roleA = activeReservedRole("INQUILINO");
        Rol roleB = activeReservedRole("PROPIETARIO");
        rolUsuRepository.saveAllAndFlush(List.of(new RolUsu(currentEntity, roleA), new RolUsu(currentEntity, roleB)));

        Menu menuA = menuRepository.saveAndFlush(new Menu("A-CONTEXT-MENU", "users", (short) 1));
        Menu sharedMenu = menuRepository.saveAndFlush(new Menu("B-CONTEXT-COMPARTIDO", "grid", (short) 1));
        Menu menuC = menuRepository.saveAndFlush(new Menu("C-CONTEXT-MENU", null, (short) 1));
        Menu inactiveMenu = menuRepository.saveAndFlush(new Menu("Z-CONTEXT-MENU-INACTIVO", null, (short) 0));
        rolMeRepository.saveAllAndFlush(List.of(new RolMe(roleA, menuA), new RolMe(roleA, sharedMenu),
                new RolMe(roleA, inactiveMenu), new RolMe(roleB, sharedMenu), new RolMe(roleB, menuC)));

        Proceso processA = procesoRepository.saveAndFlush(new Proceso("A-CONTEXT-PROCESO", "context-a-" + current.codper(), (short) 1));
        Proceso sharedProcess = procesoRepository.saveAndFlush(new Proceso("B-CONTEXT-PROCESO", "context-b-" + current.codper(), (short) 1));
        Proceso inactiveProcess = procesoRepository.saveAndFlush(new Proceso("Z-CONTEXT-PROCESO-INACTIVO", "context-z-" + current.codper(), (short) 0));
        meProRepository.saveAllAndFlush(List.of(new MePro(menuA, processA), new MePro(sharedMenu, sharedProcess),
                new MePro(menuA, inactiveProcess)));

        String response = mockMvc.perform(get(CONTEXT_URL).param("login", other.login())
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(current.login()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.login").value(current.login()))
                .andExpect(jsonPath("$.usuario.codper").value(current.codper()))
                .andExpect(jsonPath("$.persona.nombre").value("Persona context.actual"))
                .andExpect(jsonPath("$.persona.foto").value("https://example.test/foto.png"))
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0].nombre").value("INQUILINO"))
                .andExpect(jsonPath("$.roles[0].menus.length()").value(2))
                .andExpect(jsonPath("$.roles[0].menus[0].nombre").value("A-CONTEXT-MENU"))
                .andExpect(jsonPath("$.roles[0].menus[0].procesos[0].enlace").value("context-a-" + current.codper()))
                .andExpect(jsonPath("$.roles[0].menus[1].codm").value(sharedMenu.getCodm()))
                .andExpect(jsonPath("$.roles[1].nombre").value("PROPIETARIO"))
                .andExpect(jsonPath("$.roles[1].menus[0].codm").value(sharedMenu.getCodm()))
                .andExpect(jsonPath("$.roles[1].menus[1].nombre").value("C-CONTEXT-MENU"))
                .andExpect(jsonPath("$.roles[1].menus[1].procesos").isEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.persona.correo").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("Z-CONTEXT-MENU-INACTIVO",
                "Z-CONTEXT-PROCESO-INACTIVO", other.login());

        rolMeRepository.deleteById(new RolMeId(roleA.getCodr(), menuA.getCodm()));
        meProRepository.deleteById(new MeProId(sharedMenu.getCodm(), sharedProcess.getCodp()));
        rolMeRepository.flush();
        meProRepository.flush();

        String changedResponse = mockMvc.perform(get(CONTEXT_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(current.login()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(changedResponse).doesNotContain("A-CONTEXT-MENU", "B-CONTEXT-PROCESO");
    }

    @Test
    void allowsTenantAndOwnerToReadTheirOwnContextIncludingNullPhoto() throws Exception {
        UsuarioResponse normal = createUser("CTX-003", "context.normal", null);
        UsuarioResponse secondTenant = createUser("CTX-004", "context.tenant.second", null);
        UsuarioResponse owner = createUser("CTX-005", "context.owner", null);
        assignRole(normal.login(), activeReservedRole("INQUILINO"));
        assignRole(secondTenant.login(), activeReservedRole("INQUILINO"));
        assignRole(owner.login(), activeReservedRole("PROPIETARIO"));

        assertContextFor(normal.login(), true);
        assertContextFor(secondTenant.login(), false);
        assertContextFor(owner.login(), false);
    }

    @Test
    void dashboardNavigationIsSeededOnceAndVisibleOnlyToTheOwnerRole() throws Exception {
        UsuarioResponse owner = createUser("CTX-DASH-OWNER", "context.dashboard.owner", null);
        UsuarioResponse tenant = createUser("CTX-DASH-TENANT", "context.dashboard.tenant", null);
        assignRole(owner.login(), activeReservedRole("PROPIETARIO"));
        assignRole(tenant.login(), activeReservedRole("INQUILINO"));

        String ownerContext = mockMvc.perform(get(CONTEXT_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(owner.login()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tenantContext = mockMvc.perform(get(CONTEXT_URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(tenant.login()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(ownerContext).contains("DASHBOARD", "RESUMEN FINANCIERO", "dashboard/resumen-financiero");
        assertThat(tenantContext).doesNotContain("DASHBOARD", "RESUMEN FINANCIERO",
                "dashboard/resumen-financiero");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM menus m
                JOIN mepro mp ON mp.codm = m.codm
                JOIN procesos p ON p.codp = mp.codp
                WHERE m.nombre = 'DASHBOARD'
                  AND p.nombre = 'RESUMEN FINANCIERO'
                  AND p.enlace = 'dashboard/resumen-financiero'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rolme rm
                JOIN menus m ON m.codm = rm.codm
                JOIN roles r ON r.codr = rm.codr
                WHERE m.nombre = 'DASHBOARD'
                  AND r.nombre <> 'PROPIETARIO'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rolme rm
                JOIN menus m ON m.codm = rm.codm
                JOIN roles r ON r.codr = rm.codr
                WHERE m.nombre = 'DASHBOARD'
                  AND r.nombre = 'PROPIETARIO'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version IN ('26', '27') AND success
                """, Integer.class)).isEqualTo(2);
    }

    private void assertContextFor(String login, boolean expectNullPhoto) throws Exception {
        var result = mockMvc.perform(get(CONTEXT_URL).header(HttpHeaders.AUTHORIZATION, bearer(login(login))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.login").value(login))
                .andExpect(jsonPath("$.roles").isNotEmpty());
        if (expectNullPhoto) {
            result.andExpect(jsonPath("$.persona.foto").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    private UsuarioResponse createUser(String ci, String login, String foto) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona " + login, null, null,
                "F", null, login + "@example.test", "70000000", "I", foto));
        return usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, null, persona.codper()));
    }

    private AuthResult login(String login) {
        return authService.login(new LoginRequest(login, PASSWORD, "context-device-" + login,
                "Context integration", com.orman.backend.auth.model.ClientType.MOBILE));
    }

    private String bearer(AuthResult result) {
        return "Bearer " + result.response().accessToken();
    }

    private Rol activeReservedRole(String nombre) {
        return rolRepository.findAll().stream().filter(role -> nombre.equals(role.getNombre()))
                .findFirst().orElseThrow();
    }

    private void assignRole(String login, Rol role) {
        Usuario user = usuarioRepository.findById(login).orElseThrow();
        rolUsuRepository.saveAndFlush(new RolUsu(user, role));
    }
}
