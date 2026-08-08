package com.orman.backend.authorization.integration;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.repository.RolUsuRepository;
import com.orman.backend.role.service.RolUsuService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import com.orman.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class OwnerAuthorizationPersistenceIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";

    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private RolUsuService rolUsuService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private RolUsuRepository rolUsuRepository;

    @Test
    void queriesOwnerScopeActiveDefinitionCommonUsersAndPessimisticLock() {
        Rol ownerRole = activeRole("PROPIETARIO");
        long baseline = rolUsuRepository.countActiveOwners();
        UsuarioResponse owner = createUsuario("M112-PERSIST-O", "m112.persist.owner");
        UsuarioResponse common = createUsuario("M112-PERSIST-C", "m112.persist.common");
        rolUsuService.assign(owner.login(), ownerRole.getCodr());

        assertThat(rolRepository.findByNombreForUpdate("PROPIETARIO")).contains(ownerRole);
        assertThat(rolUsuRepository.existsActiveOwnerRoleByLogin(owner.login())).isTrue();
        assertThat(rolUsuRepository.existsActiveOwnerRoleByPerson(owner.codper())).isTrue();
        assertThat(rolUsuRepository.existsActiveOwnerByLogin(owner.login())).isTrue();
        assertThat(rolUsuRepository.existsActiveOwnerByPerson(owner.codper())).isTrue();
        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(baseline + 1);
        assertThat(usuarioRepository.findAllCommon(PageRequest.of(0, 100)).getContent())
                .extracting(Usuario::getLogin)
                .contains(common.login())
                .doesNotContain(owner.login());

        Usuario ownerEntity = usuarioRepository.findById(owner.login()).orElseThrow();
        ownerEntity.setEstado((short) 0);
        usuarioRepository.saveAndFlush(ownerEntity);
        assertThat(rolUsuRepository.existsActiveOwnerRoleByLogin(owner.login())).isTrue();
        assertThat(rolUsuRepository.existsActiveOwnerByLogin(owner.login())).isFalse();
        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(baseline);

        ownerEntity.setEstado((short) 1);
        usuarioRepository.saveAndFlush(ownerEntity);
        Persona ownerPerson = personaRepository.findById(owner.codper()).orElseThrow();
        ownerPerson.setEstado((short) 0);
        personaRepository.saveAndFlush(ownerPerson);
        assertThat(rolUsuRepository.existsActiveOwnerByPerson(owner.codper())).isFalse();
        assertThat(rolUsuRepository.countActiveOwners()).isEqualTo(baseline);
    }

    private UsuarioResponse createUsuario(String ci, String login) {
        PersonaResponse persona = personaService.create(new CreatePersonaRequest(ci, "Persona matriz", null,
                null, "F", null, "persona.owner@example.test", "70000000", "A", null));
        return usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, null, persona.codper()));
    }

    private Rol activeRole(String name) {
        Rol rol = rolRepository.findAll().stream()
                .filter(candidate -> name.equals(candidate.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    Rol created = new Rol();
                    created.setNombre(name);
                    created.setEstado((short) 1);
                    return rolRepository.saveAndFlush(created);
                });
        rol.setEstado((short) 1);
        return rolRepository.saveAndFlush(rol);
    }
}
