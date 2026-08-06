package com.orman.backend.auth.service;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;

public interface UserAuthorityService {

    List<GrantedAuthority> loadAuthorities(String login);
}
