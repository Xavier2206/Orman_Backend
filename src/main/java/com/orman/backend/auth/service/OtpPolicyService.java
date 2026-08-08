package com.orman.backend.auth.service;

import com.orman.backend.auth.model.ClientType;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public interface OtpPolicyService {

    boolean requiresOtp(ClientType clientType, Collection<? extends GrantedAuthority> authorities);
}
