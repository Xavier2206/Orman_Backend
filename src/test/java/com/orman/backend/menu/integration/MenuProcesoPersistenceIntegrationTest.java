package com.orman.backend.menu.integration;

import com.orman.backend.menu.entity.MePro;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.dto.request.CreateMenuRequest;
import com.orman.backend.menu.dto.response.MenuResumenResponse;
import com.orman.backend.menu.dto.response.MenuResponse;
import com.orman.backend.menu.repository.MeProRepository;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.menu.service.MeProService;
import com.orman.backend.menu.service.MenuService;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.repository.ProcesoRepository;
import com.orman.backend.process.service.ProcesoService;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolMe;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolMeRepository;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.role.service.RolMeService;
import com.orman.backend.process.dto.request.CreateProcesoRequest;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class MenuProcesoPersistenceIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MenuRepository menuRepository;
    @Autowired private ProcesoRepository procesoRepository;
    @Autowired private RolMeRepository rolMeRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private MeProRepository meProRepository;
    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private RolService rolService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private MenuService menuService;
    @Autowired private ProcesoService procesoService;
    @Autowired private RolMeService rolMeService;
    @Autowired private MeProService meProService;

    @Test
    void administrativeServicesPersistRelationsAndKeepPrincipalEntitiesWhenRemoved() {
        RolResponse rol = rolService.create(new CreateRolRequest("ROL-ADMIN-REL", null));
        var menu = menuService.create(new CreateMenuRequest("MENU-ADMIN-REL", "users", null));
        var proceso = procesoService.create(new CreateProcesoRequest("PROCESO-ADMIN-REL", "admin-rel", null));
        assertThat(rolMeService.assign(rol.codr(), menu.codm()).estadoMenu()).isEqualTo((short) 1);
        assertThat(meProService.assign(menu.codm(), proceso.codp()).estadoProceso()).isEqualTo((short) 1);
        assertThat(rolMeService.listByRol(rol.codr())).hasSize(1);
        assertThat(meProService.listByMenu(menu.codm())).hasSize(1);
        rolMeService.remove(rol.codr(), menu.codm());
        meProService.remove(menu.codm(), proceso.codp());
        assertThat(menuRepository.findById(menu.codm())).isPresent();
        assertThat(procesoRepository.findById(proceso.codp())).isPresent();
    }

    @Test
    void v7CreatesApprovedMenuProcesoSchemaWithSubsequentMigrations() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);
        assertThat(tables).contains("menus", "procesos", "rolme", "mepro").doesNotContain("rolpro");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version IN ('1','2','3','4','5','6','7','8','9','10','11','12') AND success
                """, Integer.class)).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT version FROM flyway_schema_history
                WHERE success ORDER BY installed_rank DESC LIMIT 1
                """, String.class)).isEqualTo("12");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '7' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '8' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '9' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '10' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '11' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '12' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'menus'::regclass", String.class))
                .containsExactlyInAnyOrder("pk_menus", "uk_menus_nombre", "ck_menus_estado");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'procesos'::regclass", String.class))
                .containsExactlyInAnyOrder("pk_procesos", "uk_procesos_nombre", "uk_procesos_enlace", "ck_procesos_estado");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'rolme'::regclass", String.class))
                .containsExactlyInAnyOrder("pk_rolme", "fk_rolme_codr", "fk_rolme_codm");
        assertThat(jdbcTemplate.queryForList("SELECT conname FROM pg_constraint WHERE conrelid = 'mepro'::regclass", String.class))
                .containsExactlyInAnyOrder("pk_mepro", "fk_mepro_codm", "fk_mepro_codp");
    }

    @Test
    void persistsMenuAndProcesoWithDatabaseDefaultsAndConstraints() {
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-PRUEBA", null, null));
        Proceso proceso = procesoRepository.saveAndFlush(new Proceso("PROCESO-PRUEBA", "proceso-prueba", null));

        assertThat(menu.getCodm()).isNotNull();
        assertThat(proceso.getCodp()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM menus WHERE codm = ?", Short.class, menu.getCodm()))
                .isEqualTo((short) 1);
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM procesos WHERE codp = ?", Short.class, proceso.getCodp()))
                .isEqualTo((short) 1);
        assertThat(menuRepository.existsByNombre("MENU-PRUEBA")).isTrue();
        assertThat(procesoRepository.existsByNombre("PROCESO-PRUEBA")).isTrue();
        assertThat(procesoRepository.existsByEnlace("proceso-prueba")).isTrue();

    }

    @Test
    void persistsExplicitRelationsAndRejectsDuplicatesOrMissingForeignKeys() {
        RolResponse rol = rolService.create(new CreateRolRequest("ROL-MENUS", null));
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-RELACION", "menu", null));
        Proceso proceso = procesoRepository.saveAndFlush(new Proceso("PROCESO-RELACION", "proceso-relacion", null));

        RolMe rolMe = rolMeRepository.saveAndFlush(new RolMe(rolEntity(rol.codr()), menu));
        MePro mePro = meProRepository.saveAndFlush(new MePro(menu, proceso));
        assertThat(rolMe.getId().getCodr()).isEqualTo(rol.codr());
        assertThat(mePro.getId().getCodp()).isEqualTo(proceso.getCodp());

        rolMeRepository.delete(rolMe);
        meProRepository.delete(mePro);
        rolMeRepository.flush();
        meProRepository.flush();
        assertThat(menuRepository.findById(menu.getCodm())).isPresent();
        assertThat(procesoRepository.findById(proceso.getCodp())).isPresent();
        assertThat(rolRepository.findById(rol.codr())).isPresent();
    }

    @Test
    void rejectsDuplicateMenuName() {
        menuRepository.saveAndFlush(new Menu("MENU-DUPLICADO", null, null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO menus (nombre) VALUES ('MENU-DUPLICADO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidMenuState() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO menus (nombre, estado) VALUES ('MENU-INVALIDO', 2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateProcesoEnlace() {
        procesoRepository.saveAndFlush(new Proceso("PROCESO-DUPLICADO", "proceso-duplicado", null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO procesos (nombre, enlace) VALUES ('OTRO', 'proceso-duplicado')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateProcesoName() {
        procesoRepository.saveAndFlush(new Proceso("PROCESO-NOMBRE-DUPLICADO", "proceso-nombre-uno", null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO procesos (nombre, enlace) VALUES ('PROCESO-NOMBRE-DUPLICADO', 'proceso-nombre-dos')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidProcesoState() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO procesos (nombre, enlace, estado) VALUES ('PROCESO-INVALIDO', 'invalido', 2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateAndMissingForeignKeysForRolMe() {
        RolResponse rol = rolService.create(new CreateRolRequest("ROL-FK", null));
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-FK", null, null));
        rolMeRepository.saveAndFlush(new RolMe(rolEntity(rol.codr()), menu));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO rolme (codr, codm) VALUES (?, ?)", rol.codr(), menu.getCodm()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingForeignKeyForRolMe() {
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-FK-AUSENTE", null, null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO rolme (codr, codm) VALUES (999999, ?)", menu.getCodm()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingMenuForeignKeyForRolMe() {
        RolResponse rol = rolService.create(new CreateRolRequest("ROL-MENU-AUSENTE", null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO rolme (codr, codm) VALUES (?, 999999)", rol.codr()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateAndMissingForeignKeysForMePro() {
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-MEPRO", null, null));
        Proceso proceso = procesoRepository.saveAndFlush(new Proceso("PROCESO-MEPRO", "mepro", null));
        meProRepository.saveAndFlush(new MePro(menu, proceso));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO mepro (codm, codp) VALUES (?, ?)", menu.getCodm(), proceso.getCodp()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingForeignKeyForMePro() {
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-MEPRO-AUSENTE", null, null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO mepro (codm, codp) VALUES (?, 999999)", menu.getCodm()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingMenuForeignKeyForMePro() {
        Proceso proceso = procesoRepository.saveAndFlush(new Proceso("PROCESO-MENU-AUSENTE", "menu-ausente", null));
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO mepro (codm, codp) VALUES (999999, ?)", proceso.getCodp()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void traversesApprovedPersistentFlowWithoutRoleProcessRelation() {
        UsuarioResponse usuario = createUsuario("FLOW-CI", "flow.usuario");
        RolResponse rol = rolService.create(new CreateRolRequest("ROL-FLOW", null));
        rolUsuService.assign(usuario.login(), rol.codr());
        Menu menu = menuRepository.saveAndFlush(new Menu("MENU-FLOW", null, null));
        Proceso proceso = procesoRepository.saveAndFlush(new Proceso("PROCESO-FLOW", "flow", null));
        rolMeRepository.saveAndFlush(new RolMe(rolEntity(rol.codr()), menu));
        meProRepository.saveAndFlush(new MePro(menu, proceso));

        assertThat(rolMeRepository.findByIdCodr(rol.codr())).extracting(relation -> relation.getMenu().getCodm())
                .containsExactly(menu.getCodm());
        assertThat(meProRepository.findByIdCodm(menu.getCodm())).extracting(relation -> relation.getProceso().getCodp())
                .containsExactly(proceso.getCodp());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rolusu WHERE login = ? AND codr = ?", Integer.class,
                usuario.login(), rol.codr())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'rolpro'", Integer.class))
                .isZero();
    }

    @Test
    void searchesMenusInPostgreSqlWithCombinedFiltersAndBlankQuery() {
        String marker = "QMENU" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        MenuResponse activeControl = menuService.create(new CreateMenuRequest("CONTROL DE ACCESO " + marker,
                "shield", (short) 1));
        MenuResponse inactiveControl = menuService.create(new CreateMenuRequest("CONTROL INACTIVO " + marker,
                "shield_off", (short) 0));
        MenuResponse personas = menuService.create(new CreateMenuRequest("PERSONAS " + marker, "users", (short) 1));
        MenuResponse reportes = menuService.create(new CreateMenuRequest("REPORTES " + marker, "assessment", (short) 1));
        var pageable = PageRequest.of(0, 100, Sort.by("nombre").ascending());

        assertThat(menuService.list("control", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm(), inactiveControl.codm());
        assertThat(menuService.list("acceso", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm());
        assertThat(menuService.list("pers", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(personas.codm());
        assertThat(menuService.list("CoNtRoL", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm(), inactiveControl.codm());
        assertThat(menuService.list(" control ", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm(), inactiveControl.codm());
        assertThat(menuService.list("   ", null, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm(), inactiveControl.codm(), personas.codm(), reportes.codm());

        assertThat(menuService.list(null, (short) 1, pageable).content()).extracting(MenuResponse::estado)
                .containsOnly((short) 1);
        assertThat(menuService.list(null, (short) 0, pageable).content()).extracting(MenuResponse::estado)
                .containsOnly((short) 0);
        assertThat(menuService.list(marker, null, pageable).content()).extracting(MenuResponse::codm)
                .containsExactly(activeControl.codm(), inactiveControl.codm(), personas.codm(), reportes.codm());
        assertThat(menuService.list("control", (short) 1, pageable).content()).extracting(MenuResponse::codm)
                .contains(activeControl.codm());
        assertThat(menuService.list("control", (short) 0, pageable).content()).extracting(MenuResponse::codm)
                .contains(inactiveControl.codm());
        var empty = menuService.list("SIN RESULTADOS " + marker, (short) 1, pageable);
        assertThat(empty.content()).isEmpty();
        assertThat(empty.totalElements()).isZero();
    }

    @Test
    void paginatesOnlyTheFilteredMenuUniverseAndCalculatesGlobalResumen() {
        String marker = "QPAGMENU" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        MenuResumenResponse before = menuService.resumen();
        menuService.create(new CreateMenuRequest("CONTROL PAG A " + marker, null, (short) 1));
        menuService.create(new CreateMenuRequest("CONTROL PAG B " + marker, null, (short) 1));
        menuService.create(new CreateMenuRequest("CONTROL PAG C " + marker, null, (short) 1));
        menuService.create(new CreateMenuRequest("INACTIVO PAG A " + marker, null, (short) 0));
        menuService.create(new CreateMenuRequest("INACTIVO PAG B " + marker, null, (short) 0));

        var page = menuService.list(marker, (short) 1,
                PageRequest.of(1, 2, Sort.by("nombre").ascending()));
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.first()).isFalse();
        assertThat(page.last()).isTrue();

        assertThat(menuService.resumen()).isEqualTo(new MenuResumenResponse(before.totalMenus() + 5,
                before.activos() + 3, before.inactivos() + 2));
    }

    private Rol rolEntity(Integer codr) {
        return rolRepository.findById(codr).orElseThrow();
    }

    private UsuarioResponse createUsuario(String ci, String login) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona prueba", null, null,
                "F", null, "persona@example.test", "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, "clave-ficticia", null, persona.codper()));
    }
}
