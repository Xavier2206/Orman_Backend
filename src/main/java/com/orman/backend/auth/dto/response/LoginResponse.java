package com.orman.backend.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String login, Integer codper, String accessToken, String refreshToken,
        String tokenType, long expiresIn, UUID sid) {

    @Override
    public String toString() {
        return "LoginResponse[login=" + login + ", codper=" + codper
                + ", accessToken=<redacted>, refreshToken=<redacted>, tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", sid=" + sid + "]";
    }
}
