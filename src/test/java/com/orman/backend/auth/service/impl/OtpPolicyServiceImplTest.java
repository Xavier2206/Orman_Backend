package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.model.ClientType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class OtpPolicyServiceImplTest {

    private final OtpPolicyServiceImpl policy = new OtpPolicyServiceImpl();

    @Test
    void requiresOtpOnlyForWebWithAnActiveAdministrativeAuthority() {
        assertThat(policy.requiresOtp(ClientType.WEB, authorities("ROLE_PROPIETARIO"))).isTrue();
        assertThat(policy.requiresOtp(ClientType.WEB, authorities("ROLE_ADMINISTRADOR"))).isTrue();
        assertThat(policy.requiresOtp(ClientType.WEB, authorities("ROLE_INQUILINO", "ROLE_ADMINISTRADOR"))).isTrue();
        assertThat(policy.requiresOtp(ClientType.WEB, authorities("ROLE_INQUILINO"))).isFalse();
        assertThat(policy.requiresOtp(ClientType.WEB, List.of())).isFalse();
        assertThat(policy.requiresOtp(ClientType.MOBILE, authorities("ROLE_PROPIETARIO"))).isFalse();
        assertThat(policy.requiresOtp(ClientType.MOBILE, authorities("ROLE_ADMINISTRADOR"))).isFalse();
        assertThat(policy.requiresOtp(ClientType.MOBILE, authorities("ROLE_INQUILINO"))).isFalse();
    }

    private List<SimpleGrantedAuthority> authorities(String... values) {
        return java.util.Arrays.stream(values).map(SimpleGrantedAuthority::new).toList();
    }
}
