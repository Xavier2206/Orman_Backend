package com.orman.backend.auth.service;

import java.time.Instant;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(String login, UUID sid);

    JwtClaims validateAndExtract(String token);

    record JwtClaims(String subject, UUID sid, String issuer, Instant issuedAt, Instant expiresAt) {
    }
}
