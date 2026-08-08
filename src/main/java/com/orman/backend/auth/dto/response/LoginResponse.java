package com.orman.backend.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(String status, UUID challengeId, String login, Integer codper, String accessToken,
        String refreshToken, String tokenType, long expiresIn, UUID sid) {

    public LoginResponse(String login, Integer codper, String accessToken, String refreshToken,
            String tokenType, long expiresIn, UUID sid) {
        this("AUTHENTICATED", null, login, codper, accessToken, refreshToken, tokenType, expiresIn, sid);
    }

    public static LoginResponse otpRequired(UUID challengeId, long expiresIn) {
        return new LoginResponse("OTP_REQUIRED", challengeId, null, null, null, null, null, expiresIn, null);
    }

    @Override
    public String toString() {
        return "LoginResponse[status=" + status + ", challengeId=" + challengeId + ", login=" + login + ", codper=" + codper
                + ", accessToken=<redacted>, refreshToken=<redacted>, tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", sid=" + sid + "]";
    }
}
