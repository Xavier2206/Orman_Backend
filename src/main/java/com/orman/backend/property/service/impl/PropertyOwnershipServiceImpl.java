package com.orman.backend.property.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyOwnershipServiceImpl implements PropertyOwnershipService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Persona currentPropietaria(Authentication authentication) {
        String login = authenticatedLogin(authentication);
        Usuario usuario = usuarioRepository.findByLoginWithPersona(login)
                .orElseThrow(this::accessDenied);
        return usuario.getPersona();
    }

    @Override
    public void assertCurrentPropietaria(Authentication authentication, Persona propietaria) {
        assertCurrentPropietaria(authentication, propietaria.getCodper());
    }

    @Override
    public void assertCurrentPropietaria(Authentication authentication, Integer codperPropietaria) {
        if (!currentPropietaria(authentication).getCodper().equals(codperPropietaria)) {
            throw accessDenied();
        }
    }

    private String authenticatedLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw accessDenied();
        }
        return user.login();
    }

    private AccessDeniedException accessDenied() {
        return new AccessDeniedException("No tiene autorización para administrar esta Propiedad.");
    }
}
