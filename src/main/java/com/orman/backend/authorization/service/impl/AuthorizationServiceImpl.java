package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.authorization.service.AuthorizationService;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.user.repository.UsuarioRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String OWNER_AUTHORITY = "ROLE_PROPIETARIO";
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;

    @Override
    public boolean isOwner(Authentication authentication) {
        return hasAuthority(authentication, OWNER_AUTHORITY);
    }

    @Override
    public boolean isSelfOrOwner(Authentication authentication, String login) {
        return isOwner(authentication) || authenticatedLogin(authentication).map(login::equals).orElse(false);
    }

    @Override
    public boolean canManageUser(Authentication authentication, String login) {
        if (login == null || !isOwner(authentication)) {
            return false;
        }
        Optional<String> actorLogin = authenticatedLogin(authentication);
        if (actorLogin.filter(login::equals).isPresent()) {
            return true;
        }
        return usuarioRepository.findByLoginWithPersona(login)
                .map(usuario -> canManagePerson(authentication, usuario.getPersona().getCodper()))
                .orElse(false);
    }

    @Override
    public boolean canManagePerson(Authentication authentication, Integer codper) {
        if (codper == null || !isOwner(authentication)) {
            return false;
        }
        return authenticatedLogin(authentication)
                .flatMap(usuarioRepository::findByLoginWithPersona)
                .map(usuario -> {
                    Integer ownerCodper = usuario.getPersona().getCodper();
                    return ownerCodper.equals(codper)
                            || personaRepository.existsByCodperAndCreadaPorLogin(codper, usuario.getLogin())
                            || personaRepository.existsTenantLinkedToOwner(codper, ownerCodper);
                })
                .orElse(false);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private Optional<String> authenticatedLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user.login());
    }
}
