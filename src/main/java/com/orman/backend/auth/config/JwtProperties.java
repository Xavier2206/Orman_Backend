package com.orman.backend.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @Min(1) long accessTokenExpirationMinutes,
        @Min(1) long refreshTokenExpirationDays) {

    public JwtProperties {
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret debe contener al menos 32 bytes.");
        }
    }

    public long accessTokenExpirationSeconds() {
        return Math.multiplyExact(accessTokenExpirationMinutes, 60L);
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=<redacted>, issuer=" + issuer
                + ", accessTokenExpirationMinutes=" + accessTokenExpirationMinutes
                + ", refreshTokenExpirationDays=" + refreshTokenExpirationDays + "]";
    }
}
