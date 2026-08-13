package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.response.AuthContextResponse;
import com.orman.backend.auth.model.AuthenticatedUser;

public interface AuthContextService {

    AuthContextResponse getCurrentContext(AuthenticatedUser authenticatedUser);
}
