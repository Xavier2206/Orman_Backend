package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.role.repository.RolUsuRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock private RolUsuRepository rolUsuRepository;
    @InjectMocks private AuthorizationServiceImpl service;

    @Test
    void ownerCanManageEveryTargetAndChangeAnyPassword() {
        Authentication owner = authentication("owner", "ROLE_PROPIETARIO");

        assertThat(service.isOwner(owner)).isTrue();
        assertThat(service.canManageUser(owner, "another.owner")).isTrue();
        assertThat(service.canManagePerson(owner, 99)).isTrue();
        assertThat(service.isSelfOrOwner(owner, "another.user")).isTrue();
    }

    @Test
    void administratorCanManageOnlyTargetsWithoutActiveOwnerRole() {
        Authentication administrator = authentication("admin", "ROLE_ADMINISTRADOR");
        when(rolUsuRepository.existsActiveOwnerRoleByLogin("common")).thenReturn(false);
        when(rolUsuRepository.existsActiveOwnerRoleByLogin("owner")).thenReturn(true);
        when(rolUsuRepository.existsActiveOwnerRoleByPerson(10)).thenReturn(false);
        when(rolUsuRepository.existsActiveOwnerRoleByPerson(11)).thenReturn(true);

        assertThat(service.canManageUser(administrator, "common")).isTrue();
        assertThat(service.canManageUser(administrator, "owner")).isFalse();
        assertThat(service.canManagePerson(administrator, 10)).isTrue();
        assertThat(service.canManagePerson(administrator, 11)).isFalse();
        assertThat(service.isSelfOrOwner(administrator, "another.user")).isFalse();
        assertThat(service.isSelfOrOwner(administrator, "admin")).isTrue();
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
