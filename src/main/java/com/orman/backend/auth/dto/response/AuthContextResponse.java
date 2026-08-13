package com.orman.backend.auth.dto.response;

import java.util.List;

public record AuthContextResponse(AuthContextUsuarioResponse usuario, AuthContextPersonaResponse persona,
        List<AuthContextRolResponse> roles) {
}
