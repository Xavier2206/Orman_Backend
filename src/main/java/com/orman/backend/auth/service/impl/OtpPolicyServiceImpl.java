package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.service.OtpPolicyService;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class OtpPolicyServiceImpl implements OtpPolicyService {

    private static final Set<String> ADMINISTRATIVE_AUTHORITIES = Set.of("ROLE_PROPIETARIO", "ROLE_ADMINISTRADOR");

    @Override
    public boolean requiresOtp(ClientType clientType, Collection<? extends GrantedAuthority> authorities) {
        return clientType == ClientType.WEB && authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMINISTRATIVE_AUTHORITIES::contains);
    }
}
