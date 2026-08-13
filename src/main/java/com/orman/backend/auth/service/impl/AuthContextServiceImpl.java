package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.dto.response.AuthContextResponse;
import com.orman.backend.auth.mapper.AuthContextMapper;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.repository.AuthContextRepository;
import com.orman.backend.auth.service.AuthContextService;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthContextServiceImpl implements AuthContextService {

    private final UsuarioRepository usuarioRepository;
    private final AuthContextRepository authContextRepository;
    private final AuthContextMapper authContextMapper;

    @Override
    @Transactional(readOnly = true)
    public AuthContextResponse getCurrentContext(AuthenticatedUser authenticatedUser) {
        Usuario usuario = usuarioRepository.findByLoginWithPersona(authenticatedUser.login())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        return authContextMapper.toResponse(usuario,
                authContextRepository.findActiveNavigationByLogin(authenticatedUser.login()));
    }
}
