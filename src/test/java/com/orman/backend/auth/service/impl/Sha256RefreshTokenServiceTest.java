package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.service.RefreshTokenService;
import java.security.SecureRandom;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256RefreshTokenServiceTest {

    private final Sha256RefreshTokenService service = new Sha256RefreshTokenService(new SecureRandom());

    @Test
    void generatesOpaqueRandomTokenAndStoresComparableSha256Only() {
        UUID sid = UUID.fromString("4c92a154-070c-45e4-bbcc-8a2ae1c697cb");

        RefreshTokenService.GeneratedRefreshToken first = service.generate(sid);
        RefreshTokenService.GeneratedRefreshToken second = service.generate(sid);

        assertThat(first.value()).startsWith(sid + ".").isNotEqualTo(second.value());
        assertThat(first.hash()).hasSize(43).isNotEqualTo(first.value()).isNotEqualTo(second.hash());
        assertThat(service.extractSid(first.value())).isEqualTo(sid);
        assertThat(service.matches(first.value(), first.hash())).isTrue();
        assertThat(service.matches(second.value(), first.hash())).isFalse();
    }

    @Test
    void rejectsMalformedTokensWithoutLeakingDetails() {
        assertThatThrownBy(() -> service.extractSid(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.extractSid("not-a-token")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.extractSid("4c92a154-070c-45e4-bbcc-8a2ae1c697cb.a.b"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
