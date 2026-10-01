package com.orman.backend.authorization.service;

import org.springframework.security.core.Authentication;

public interface AuthorizationService {

    boolean isOwner(Authentication authentication);

    boolean isSelfOrOwner(Authentication authentication, String login);

    boolean canManageUser(Authentication authentication, String login);

    boolean canManagePerson(Authentication authentication, Integer codper);
}
