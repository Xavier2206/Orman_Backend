package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

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
