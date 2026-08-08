package com.orman.backend.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.otp")
public record OtpProperties(
        @NotBlank String hmacSecret,
        @Min(1) long expirationSeconds,
        @Min(1) int maxAttempts,
        @Min(1) long resendCooldownSeconds,
        @Min(0) int maxResends) {

    public OtpProperties {
        if (hmacSecret != null && hmacSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.otp.hmac-secret debe contener al menos 32 bytes.");
        }
    }

    @Override
    public String toString() {
        return "OtpProperties[hmacSecret=<redacted>, expirationSeconds=" + expirationSeconds
                + ", maxAttempts=" + maxAttempts + ", resendCooldownSeconds=" + resendCooldownSeconds
                + ", maxResends=" + maxResends + "]";
    }
}
