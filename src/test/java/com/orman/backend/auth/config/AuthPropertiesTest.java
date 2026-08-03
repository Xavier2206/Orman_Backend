package com.orman.backend.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPropertiesTest {

    @Test
    void rejectsWeakJwtSecretAndUnsafeSameSiteNoneCookie() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "issuer", 15, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new RefreshCookieProperties("refresh", false, "None"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secure");
        assertThatThrownBy(() -> new RefreshCookieProperties("refresh", true, "Invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
