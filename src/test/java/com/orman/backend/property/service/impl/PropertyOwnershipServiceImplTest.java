package com.orman.backend.property.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyOwnershipServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private Authentication authentication;
    @InjectMocks private PropertyOwnershipServiceImpl service;

    @Test
    void resolvesThePersonaOfTheAuthenticatedUserAndRejectsAnotherOwner() {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", 7);
        Usuario usuario = new Usuario();
        usuario.setPersona(persona);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new AuthenticatedUser("propietario", UUID.randomUUID()));
        when(usuarioRepository.findByLoginWithPersona("propietario")).thenReturn(Optional.of(usuario));

        assertThat(service.currentPropietaria(authentication)).isSameAs(persona);
        service.assertCurrentPropietaria(authentication, 7);
        assertThatThrownBy(() -> service.assertCurrentPropietaria(authentication, 8))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnAuthenticationWithoutTheExpectedPrincipal() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("propietario");

        assertThatThrownBy(() -> service.currentPropietaria(authentication))
                .isInstanceOf(AccessDeniedException.class);
    }
}
