package com.orman.backend.auth.service;

import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.model.ClientType;

public record AuthResult(LoginResponse response, String refreshToken, ClientType clientType) {

    @Override
    public String toString() {
        return "AuthResult[response=<redacted>, refreshToken=<redacted>, clientType=" + clientType + "]";
    }
}
