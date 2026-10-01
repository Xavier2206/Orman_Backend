package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaRepository personaRepository;
    @InjectMocks private AuthorizationServiceImpl service;

    @Test
    void ownerCanManageSelfAndLinkedTenantButNotUnrelatedPerson() {
        Authentication owner = authentication("owner", "ROLE_PROPIETARIO");
        Usuario usuario = new Usuario();
        usuario.setLogin("owner");
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", 7);
        usuario.setPersona(persona);
        when(usuarioRepository.findByLoginWithPersona("owner")).thenReturn(Optional.of(usuario));
        when(personaRepository.existsTenantLinkedToOwner(10, 7)).thenReturn(true);
        Persona manageablePersona = new Persona();
        ReflectionTestUtils.setField(manageablePersona, "codper", 10);
        Usuario manageableUser = new Usuario();
        manageableUser.setLogin("managed.tenant");
        manageableUser.setPersona(manageablePersona);
        when(usuarioRepository.findByLoginWithPersona("managed.tenant")).thenReturn(Optional.of(manageableUser));
        when(personaRepository.existsByCodperAndCreadaPorLogin(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.eq("owner")))
                .thenAnswer(invocation -> Integer.valueOf(11).equals(invocation.getArgument(0)));

        assertThat(service.isOwner(owner)).isTrue();
        assertThat(service.canManageUser(owner, "owner")).isTrue();
        assertThat(service.canManageUser(owner, "managed.tenant")).isTrue();
        assertThat(service.canManageUser(owner, "another.owner")).isFalse();
        assertThat(service.canManagePerson(owner, 7)).isTrue();
        assertThat(service.canManagePerson(owner, 10)).isTrue();
        assertThat(service.canManagePerson(owner, 11)).isTrue();
        assertThat(service.canManagePerson(owner, 99)).isFalse();
        assertThat(service.isSelfOrOwner(owner, "another.user")).isTrue();
        verify(personaRepository).existsTenantLinkedToOwner(99, 7);
    }

    @Test
    void tenantCannotManageAnyTarget() {
        Authentication tenant = authentication("tenant", "ROLE_INQUILINO");
        assertThat(service.canManageUser(tenant, "common")).isFalse();
        assertThat(service.canManageUser(tenant, "owner")).isFalse();
        assertThat(service.canManagePerson(tenant, 10)).isFalse();
        assertThat(service.canManagePerson(tenant, 11)).isFalse();
        assertThat(service.isSelfOrOwner(tenant, "another.user")).isFalse();
        assertThat(service.isSelfOrOwner(tenant, "tenant")).isTrue();
    }

    @Test
    void tenantAndUserWithoutRolesCanOnlyMatchTheirOwnLogin() {
        Authentication tenant = authentication("tenant", "ROLE_INQUILINO");
        Authentication withoutRoles = authentication("plain");

        assertThat(service.canManageUser(tenant, "tenant")).isFalse();
        assertThat(service.canManagePerson(tenant, 10)).isFalse();
        assertThat(service.isSelfOrOwner(tenant, "tenant")).isTrue();
        assertThat(service.isSelfOrOwner(withoutRoles, "plain")).isTrue();
        assertThat(service.isSelfOrOwner(withoutRoles, "other")).isFalse();
    }

    private Authentication authentication(String login, String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(login, UUID.randomUUID()), null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
