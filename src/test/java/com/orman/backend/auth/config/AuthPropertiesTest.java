package com.orman.backend.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthTokenConfig.class)
            .withPropertyValues(
                    "security.jwt.issuer=test-issuer",
                    "security.jwt.access-token-expiration-minutes=15",
                    "security.jwt.refresh-token-expiration-days=30",
                    "security.cookie.refresh-name=orman_refresh",
                    "security.cookie.secure=false",
                    "security.cookie.same-site=Lax",
                    "security.cors.allowed-origins[0]=http://localhost:4200",
                    "security.otp.hmac-secret=test-only-otp-hmac-secret-at-least-32-bytes",
                    "security.otp.expiration-seconds=300",
                    "security.otp.max-attempts=5",
                    "security.otp.resend-cooldown-seconds=60",
                    "security.otp.max-resends=3",
                    "security.mail.from=no-reply@example.test");

    @Test
    void acceptsValidSecretAndRedactsItFromToString() {
        String testSecret = "test-only-secret-with-at-least-32-bytes-for-binding";

        contextRunner.withPropertyValues("security.jwt.secret=" + testSecret).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JwtProperties.class).toString()).doesNotContain(testSecret);
        });
    }

    @Test
    void rejectsAbsentSecretDuringConfigurationBinding() {
        contextRunner.run(context -> {
            assertThat(context.getStartupFailure()).isNotNull();
            assertThat(context.getStartupFailure().getMessage()).doesNotContain("test-only-secret");
        });
    }

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
        assertThatThrownBy(() -> new CorsProperties(java.util.List.of("*")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no permite");
    }

    @Test
    void validatesOtpSecretAndLoadsApprovedValues() {
        assertThatThrownBy(() -> new OtpProperties("too-short", 300, 5, 60, 3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 bytes");
        contextRunner.withPropertyValues("security.jwt.secret=test-only-secret-with-at-least-32-bytes-for-binding")
                .run(context -> {
                    OtpProperties properties = context.getBean(OtpProperties.class);
                    assertThat(properties.expirationSeconds()).isEqualTo(300);
                    assertThat(properties.maxAttempts()).isEqualTo(5);
                    assertThat(properties.resendCooldownSeconds()).isEqualTo(60);
                    assertThat(properties.maxResends()).isEqualTo(3);
                    assertThat(properties.toString()).doesNotContain(properties.hmacSecret());
                });
    }
}
