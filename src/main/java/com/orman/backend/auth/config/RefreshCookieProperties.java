package com.orman.backend.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.cookie")
public record RefreshCookieProperties(@NotBlank String refreshName, boolean secure, @NotBlank String sameSite) {

    public RefreshCookieProperties {
        if (sameSite != null && !sameSite.equals("Strict") && !sameSite.equals("Lax") && !sameSite.equals("None")) {
            throw new IllegalArgumentException("security.cookie.same-site debe ser Strict, Lax o None.");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requiere una cookie Secure.");
        }
    }
}
