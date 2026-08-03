package com.orman.backend.auth.service;

import java.util.UUID;

public interface RefreshTokenService {

    GeneratedRefreshToken generate(UUID sid);

    UUID extractSid(String token);

    String hash(String token);

    boolean matches(String token, String expectedHash);

    record GeneratedRefreshToken(String value, String hash) {

        @Override
        public String toString() {
            return "GeneratedRefreshToken[value=<redacted>, hash=<redacted>]";
        }
    }
}
