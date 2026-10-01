package com.orman.backend.push.service;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.push.dto.PushInstallationRequest;
import java.util.List;
import java.util.UUID;

public interface PushInstallationService {

    void register(AuthenticatedUser authenticatedUser, PushInstallationRequest request);

    void deactivate(AuthenticatedUser authenticatedUser);

    void deactivateSessions(List<UUID> sids);
}
