package com.orman.backend.authorization.service.impl;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.authorization.service.AuthorizationService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String OWNER_AUTHORITY = "ROLE_PROPIETARIO";

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
        return isOwner(authentication);
    }

    @Override
    public boolean canManagePerson(Authentication authentication, Integer codper) {
        return isOwner(authentication);
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
