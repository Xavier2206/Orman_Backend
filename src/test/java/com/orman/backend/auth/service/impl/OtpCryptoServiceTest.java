package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.OtpProperties;
import java.security.SecureRandom;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpCryptoServiceTest {

    private final HmacSha256OtpDigestService digestService = new HmacSha256OtpDigestService(
            new OtpProperties("test-only-otp-hmac-secret-at-least-32-bytes", 300, 5, 60, 3));

    @Test
    void generatorAlwaysReturnsSixDigitsAndPreservesLeadingZeroes() {
        SecureRandom zero = new SecureRandom() {
            @Override public int nextInt(int bound) { return 0; }
        };
        SecureRandomOtpGenerator generator = new SecureRandomOtpGenerator(zero);
        assertThat(generator.generate()).isEqualTo("000000");

        SecureRandomOtpGenerator productionGenerator = new SecureRandomOtpGenerator(new SecureRandom());
        for (int index = 0; index < 30; index++) {
            assertThat(productionGenerator.generate()).matches("\\d{6}");
        }
    }

    @Test
    void hmacDigestIsDeterministicScopedAndComparedInConstantTime() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String digest = digestService.digest(first, "004812");
        assertThat(digest).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(digestService.digest(first, "004812")).isEqualTo(digest);
        assertThat(digestService.digest(first, "004813")).isNotEqualTo(digest);
        assertThat(digestService.digest(second, "004812")).isNotEqualTo(digest);
        assertThat(digestService.matches(first, "004812", digest)).isTrue();
        assertThat(digestService.matches(first, "004813", digest)).isFalse();
    }
}
