package com.orman.backend.menu.integration;

import com.orman.backend.menu.entity.MePro;
import com.orman.backend.menu.entity.Menu;
import com.orman.backend.menu.repository.MeProRepository;
import com.orman.backend.menu.repository.MenuRepository;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.repository.ProcesoRepository;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.entity.RolMe;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolMeRepository;
import com.orman.backend.role.service.RolService;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import java.util.List;
import java.util.Map;
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

    @Test
    void v7CreatesOnlyApprovedTablesConstraintsAndNoInitialData() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);
        assertThat(tables).contains("menus", "procesos", "rolme", "mepro").doesNotContain("rolpro");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version IN ('1','2','3','4','5','6','7') AND success
                """, Integer.class)).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT version FROM flyway_schema_history
                WHERE success ORDER BY installed_rank DESC LIMIT 1
                """, String.class)).isEqualTo("7");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '8'", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM menus", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM procesos", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rolme", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mepro", Integer.class)).isZero();
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

    private Rol rolEntity(Integer codr) {
        return rolRepository.findById(codr).orElseThrow();
    }

    private UsuarioResponse createUsuario(String ci, String login) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona prueba", null, null,
                "F", null, null, "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, "clave-ficticia", null, persona.codper()));
    }
}
