package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.service.UserAuthorityService;
import com.orman.backend.role.repository.RolUsuRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthorityServiceImpl implements UserAuthorityService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final RolUsuRepository rolUsuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GrantedAuthority> loadAuthorities(String login) {
        return rolUsuRepository.findActiveRoleNamesByLogin(login).stream()
                .map(this::toAuthorityName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name))
                .toList();
    }

    private String toAuthorityName(String roleName) {
        if (roleName == null) {
            return null;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        String name = normalized.startsWith(ROLE_PREFIX) ? normalized.substring(ROLE_PREFIX.length()) : normalized;
        return "PROPIETARIO".equals(name) || "INQUILINO".equals(name) ? ROLE_PREFIX + name : null;
    }
}
