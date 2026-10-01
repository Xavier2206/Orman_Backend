package com.orman.backend.auth.service.impl;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.auth.event.SesionesRevocadasEvent;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

            return authenticate(usuario, persona, deviceId, deviceName, request.clientType());
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
            revokeSession(sesion, now, RevocationReason.EXPIRED);
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
            revokeSession(sesion, now, RevocationReason.REFRESH_REUSE);
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

    private AuthResult authenticate(Usuario usuario, Persona persona, String deviceId, String deviceName,
            ClientType clientType) {
        LocalDateTime now = nowUtc();
        sesionRepository.findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(usuario.getLogin(), deviceId)
                .ifPresent(previous -> {
                    previous.revoke(now, RevocationReason.REPLACED_BY_NEW_LOGIN);
                    sesionRepository.saveAndFlush(previous);
                    publishRevoked(previous.getSid());
                });
        UUID sid = UUID.randomUUID();
        RefreshTokenService.GeneratedRefreshToken refreshToken = refreshTokenService.generate(sid);
        SesionUsuario sesion = new SesionUsuario(sid, usuario, refreshToken.hash(), deviceId, deviceName,
                clientType, now, now.plusDays(jwtProperties.refreshTokenExpirationDays()));
        sesionRepository.save(sesion);
        usuario.setUltimoAcceso(OrmanTimeConfig.businessNow(clock));
        usuarioRepository.save(usuario);
        return result(usuario, persona, sesion, refreshToken.value());
    }

    private LocalDateTime nowUtc() {
        return OrmanTimeConfig.technicalNow(clock);
    }

    private void revokeSession(SesionUsuario session, LocalDateTime now, RevocationReason reason) {
        if (!session.isRevoked()) {
            session.revoke(now, reason);
            publishRevoked(session.getSid());
        }
    }

    private void publishRevoked(UUID sid) {
        eventPublisher.publishEvent(new SesionesRevocadasEvent(java.util.List.of(sid)));
    }

    private boolean isActivo(Short estado) {
        return Short.valueOf(ACTIVO).equals(estado);
    }
}
