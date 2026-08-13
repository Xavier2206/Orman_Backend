package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.dto.response.AuthContextResponse;
import com.orman.backend.auth.mapper.AuthContextMapper;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.repository.AuthContextNavigationRow;
import com.orman.backend.auth.repository.AuthContextRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthContextServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuthContextRepository authContextRepository;
    @Mock private AuthContextMapper authContextMapper;
    @InjectMocks private AuthContextServiceImpl service;

    @Test
    void usesOnlyTheLoginContainedInTheAuthenticatedPrincipal() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("usuario.actual", UUID.randomUUID());
        Usuario usuario = new Usuario();
        List<AuthContextNavigationRow> rows = List.of(new AuthContextNavigationRow(1, "ROL", null, null, null,
                null, null, null));
        AuthContextResponse expected = org.mockito.Mockito.mock(AuthContextResponse.class);
        when(usuarioRepository.findByLoginWithPersona("usuario.actual")).thenReturn(Optional.of(usuario));
        when(authContextRepository.findActiveNavigationByLogin("usuario.actual")).thenReturn(rows);
        when(authContextMapper.toResponse(usuario, rows)).thenReturn(expected);

        assertThat(service.getCurrentContext(authenticatedUser)).isSameAs(expected);

        verify(usuarioRepository).findByLoginWithPersona("usuario.actual");
        verify(authContextRepository).findActiveNavigationByLogin("usuario.actual");
    }
}
