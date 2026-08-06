package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.response.SessionResponse;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.model.RevocationReason;
import java.util.List;
import java.util.UUID;

public interface SessionService {

    AuthenticatedUser authenticate(String login, UUID sid);

    void logout(AuthenticatedUser user);

    void logoutAll(AuthenticatedUser user);

    List<SessionResponse> list(AuthenticatedUser user);

    void revoke(AuthenticatedUser user, UUID sid);

    void revokeAll(String login, RevocationReason reason);
}
