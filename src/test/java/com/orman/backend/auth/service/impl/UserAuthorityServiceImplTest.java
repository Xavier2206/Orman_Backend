package com.orman.backend.auth.service.impl;

import com.orman.backend.role.repository.RolUsuRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthorityServiceImplTest {

    @Mock private RolUsuRepository rolUsuRepository;
    @InjectMocks private UserAuthorityServiceImpl service;

    @Test
    void normalizesPrefixesDeduplicatesAndSortsAuthorities() {
        when(rolUsuRepository.findActiveRoleNamesByLogin("usuario.demo"))
                .thenReturn(List.of(" propietario ", "ADMINISTRADOR", "role_inquilino", "PROPIETARIO"));

        List<GrantedAuthority> authorities = service.loadAuthorities("usuario.demo");

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR", "ROLE_INQUILINO", "ROLE_PROPIETARIO");
    }

    @Test
    void ignoresEmptyOrInconsistentNamesAndSupportsUsersWithoutRoles() {
        when(rolUsuRepository.findActiveRoleNamesByLogin("inconsistente"))
                .thenReturn(Arrays.asList(null, "", "   ", "ROLE_", " operador "));
        when(rolUsuRepository.findActiveRoleNamesByLogin("sin.roles")).thenReturn(List.of());

        assertThat(service.loadAuthorities("inconsistente")).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OPERADOR");
        assertThat(service.loadAuthorities("sin.roles")).isEmpty();
    }

    @Test
    void returnsAnImmutableCollection() {
        when(rolUsuRepository.findActiveRoleNamesByLogin("usuario.demo"))
                .thenReturn(List.of("PROPIETARIO"));

        List<GrantedAuthority> authorities = service.loadAuthorities("usuario.demo");

        assertThatThrownBy(() -> authorities.add(() -> "ROLE_OTRO"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
