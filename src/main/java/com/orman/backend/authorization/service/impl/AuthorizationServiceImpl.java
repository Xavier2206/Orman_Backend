package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.authorization.service.AuthorizationService;
import com.orman.backend.role.repository.RolUsuRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String OWNER_AUTHORITY = "ROLE_PROPIETARIO";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMINISTRADOR";

    private final RolUsuRepository rolUsuRepository;

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
        return isOwner(authentication)
                || hasAuthority(authentication, ADMIN_AUTHORITY)
                && !rolUsuRepository.existsActiveOwnerRoleByLogin(login);
    }

    @Override
    public boolean canManagePerson(Authentication authentication, Integer codper) {
        return isOwner(authentication)
                || hasAuthority(authentication, ADMIN_AUTHORITY)
                && !rolUsuRepository.existsActiveOwnerRoleByPerson(codper);
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
