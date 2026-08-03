package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Sha256RefreshTokenService implements RefreshTokenService {

    private static final int RANDOM_BYTES = 32;
    private final SecureRandom secureRandom;

    @Override
    public GeneratedRefreshToken generate(UUID sid) {
        byte[] secret = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(secret);
        String token = sid + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        return new GeneratedRefreshToken(token, hash(token));
    }

    @Override
    public UUID extractSid(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        int separator = token.indexOf('.');
        if (separator <= 0 || separator != token.lastIndexOf('.') || separator == token.length() - 1) {
            throw new InvalidRefreshTokenException();
        }
        try {
            return UUID.fromString(token.substring(0, separator));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    @Override
    public String hash(String token) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(token));
    }

    @Override
    public boolean matches(String token, String expectedHash) {
        if (token == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] digest(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
