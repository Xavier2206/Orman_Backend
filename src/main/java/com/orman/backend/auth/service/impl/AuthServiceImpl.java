package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.RefreshTokenService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final short ACTIVO = 1;
    private static final String TOKEN_TYPE = "Bearer";
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$CwTycUXWue0Thq9StjUM0uJ8xN9eCfdRsQkX.zAagVPq0Z2hGQ0Hq";

    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        String login = request.login().trim();
        String deviceId = request.deviceId().trim();
        String deviceName = request.deviceName().trim();
        Usuario usuario = usuarioRepository.findByLoginForUpdate(login).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            throw new InvalidCredentialsException();
        }

        try {
            Persona persona = usuario.getPersona();
            boolean passwordMatches = passwordEncoder.matches(request.password(), usuario.getPasswd());
            if (!passwordMatches || persona == null || !isActivo(usuario.getEstado()) || !isActivo(persona.getEstado())) {
                throw new InvalidCredentialsException();
            }

            LocalDateTime now = nowUtc();
            sesionRepository.findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(login, deviceId)
                    .ifPresent(previous -> {
                        previous.revoke(now, RevocationReason.REPLACED_BY_NEW_LOGIN);
                        sesionRepository.saveAndFlush(previous);
                    });

            UUID sid = UUID.randomUUID();
            RefreshTokenService.GeneratedRefreshToken refreshToken = refreshTokenService.generate(sid);
            SesionUsuario sesion = new SesionUsuario(sid, usuario, refreshToken.hash(), deviceId, deviceName,
                    request.clientType(), now, now.plusDays(jwtProperties.refreshTokenExpirationDays()));
            sesionRepository.save(sesion);
            usuario.setUltimoAcceso(now);
            usuarioRepository.save(usuario);

            return result(usuario, persona, sesion, refreshToken.value());
        } catch (EntityNotFoundException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResult refresh(String refreshToken, ClientType sourceClientType) {
        UUID sid = refreshTokenService.extractSid(refreshToken);
        SesionUsuario sesion = sesionRepository.findBySidForUpdate(sid)
                .orElseThrow(InvalidRefreshTokenException::new);
        LocalDateTime now = nowUtc();

        if (sesion.isRevoked() || sesion.getClientType() != sourceClientType) {
            throw new InvalidRefreshTokenException();
        }
        if (sesion.isExpiredAt(now)) {
            sesion.revoke(now, RevocationReason.EXPIRED);
            throw new InvalidRefreshTokenException();
        }

        Usuario usuario = sesion.getUsuario();
        Persona persona;
        try {
            persona = usuario.getPersona();
            if (persona == null || !isActivo(usuario.getEstado()) || !isActivo(persona.getEstado())) {
                throw new InvalidRefreshTokenException();
            }
        } catch (EntityNotFoundException exception) {
            throw new InvalidRefreshTokenException();
        }

        if (!refreshTokenService.matches(refreshToken, sesion.getRefreshTokenHash())) {
            sesion.revoke(now, RevocationReason.REFRESH_REUSE);
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenService.GeneratedRefreshToken rotated = refreshTokenService.generate(sid);
        sesion.rotateRefreshToken(rotated.hash(), now);
        return result(usuario, persona, sesion, rotated.value());
    }

    private AuthResult result(Usuario usuario, Persona persona, SesionUsuario sesion, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(usuario.getLogin(), sesion.getSid());
        String responseRefreshToken = sesion.getClientType() == ClientType.MOBILE ? refreshToken : null;
        LoginResponse response = new LoginResponse(usuario.getLogin(), persona.getCodper(), accessToken,
                responseRefreshToken, TOKEN_TYPE, jwtProperties.accessTokenExpirationSeconds(), sesion.getSid());
        return new AuthResult(response, refreshToken, sesion.getClientType());
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private boolean isActivo(Short estado) {
        return Short.valueOf(ACTIVO).equals(estado);
    }
}
