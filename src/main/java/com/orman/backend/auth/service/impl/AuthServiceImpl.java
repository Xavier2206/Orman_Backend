package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final short ACTIVO = 1;
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$CwTycUXWue0Thq9StjUM0uJ8xN9eCfdRsQkX.zAagVPq0Z2hGQ0Hq";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String login = request.login().trim();
        Usuario usuario = usuarioRepository.findById(login).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new InvalidCredentialsException();
        }

        try {
            boolean passwordMatches = passwordEncoder.matches(request.password(), usuario.getPasswd());
            Persona persona = usuario.getPersona();
            if (!passwordMatches || persona == null || !isActivo(usuario.getEstado()) || !isActivo(persona.getEstado())) {
                throw new InvalidCredentialsException();
            }

            usuario.setUltimoAcceso(LocalDateTime.now(clock));
            usuarioRepository.save(usuario);
            return new LoginResponse(usuario.getLogin(), persona.getCodper());
        } catch (EntityNotFoundException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private boolean isActivo(Short estado) {
        return Short.valueOf(ACTIVO).equals(estado);
    }
}
