package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.config.OtpProperties;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.request.OtpVerifyRequest;
import com.orman.backend.auth.dto.request.OtpResendRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.entity.SesionUsuario;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.RevocationReason;
import com.orman.backend.auth.repository.SesionUsuarioRepository;
import com.orman.backend.auth.repository.OtpChallengeRepository;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.RefreshTokenService;
import com.orman.backend.auth.service.OtpChallengeService;
import com.orman.backend.auth.service.OtpPolicyService;
import com.orman.backend.auth.service.UserAuthorityService;
import com.orman.backend.auth.service.OtpMailService;
import com.orman.backend.auth.exception.OtpDeliveryException;
import com.orman.backend.auth.model.OtpPurpose;
import com.orman.backend.auth.model.OtpChallengeStatus;
import com.orman.backend.common.exception.BusinessRuleException;
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
    private final UserAuthorityService userAuthorityService;
    private final OtpPolicyService otpPolicyService;
    private final OtpChallengeService otpChallengeService;
    private final OtpChallengeRepository otpChallengeRepository;
    private final OtpProperties otpProperties;
    private final OtpMailService otpMailService;
    private final Clock clock;

    @Override
    @Transactional(noRollbackFor = OtpDeliveryException.class)
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

            if (otpPolicyService.requiresOtp(request.clientType(), userAuthorityService.loadAuthorities(login))) {
                OtpChallengeService.CreatedOtpChallenge challenge = otpChallengeService.createLoginChallenge(login,
                        request.clientType(), null, null);
                try {
                    otpMailService.sendOtp(persona.getCorreo(), challenge.otp(), otpProperties.expirationSeconds());
                    otpChallengeService.markSent(challenge.challenge().getId());
                    return new AuthResult(LoginResponse.otpRequired(challenge.challenge().getId(),
                            otpProperties.expirationSeconds()), null, request.clientType());
                } catch (OtpDeliveryException exception) {
                    otpChallengeService.cancel(challenge.challenge().getId());
                    throw exception;
                }
            }
            return authenticate(usuario, persona, deviceId, deviceName, request.clientType());
        } catch (EntityNotFoundException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    @Override
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public AuthResult verifyOtp(OtpVerifyRequest request) {
        OtpChallengeService.OtpVerificationResult verification = otpChallengeService.verify(request.challengeId(), request.code());
        if (verification != OtpChallengeService.OtpVerificationResult.VERIFIED) {
            throw new BusinessRuleException("El código OTP no es válido.");
        }
        var challenge = otpChallengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new BusinessRuleException("El challenge OTP no es válido."));
        if (challenge.getClientType() != ClientType.WEB) {
            throw new BusinessRuleException("El contexto OTP no es válido.");
        }
        Usuario usuario = usuarioRepository.findByLoginForUpdate(challenge.getLogin())
                .orElseThrow(() -> new BusinessRuleException("El contexto de autenticación cambió."));
        Persona persona = usuario.getPersona();
        if (persona == null || !isActivo(usuario.getEstado()) || !isActivo(persona.getEstado())
                || !otpPolicyService.requiresOtp(ClientType.WEB, userAuthorityService.loadAuthorities(usuario.getLogin()))) {
            throw new BusinessRuleException("El contexto de autenticación cambió; inicie sesión nuevamente.");
        }
        return authenticate(usuario, persona, request.deviceId().trim(), request.deviceName().trim(), ClientType.WEB);
    }

    @Override
    @Transactional(noRollbackFor = OtpDeliveryException.class)
    public void resendOtp(OtpResendRequest request) {
        var challenge = otpChallengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new BusinessRuleException("El challenge OTP no es válido."));
        if (challenge.getClientType() != ClientType.WEB || challenge.getPurpose() != OtpPurpose.LOGIN) {
            throw new BusinessRuleException("El contexto OTP no es válido.");
        }
        OtpChallengeService.PreparedOtpResend resend = otpChallengeService.prepareResend(request.challengeId());
        Usuario usuario = usuarioRepository.findByLoginForUpdate(challenge.getLogin())
                .orElseThrow(() -> new BusinessRuleException("El contexto de autenticación cambió."));
        Persona persona = usuario.getPersona();
        if (persona == null || !isActivo(usuario.getEstado()) || !isActivo(persona.getEstado())) {
            throw new BusinessRuleException("El contexto de autenticación cambió.");
        }
        otpMailService.sendOtp(persona.getCorreo(), resend.otp(), otpProperties.expirationSeconds());
        otpChallengeService.confirmResend(request.challengeId(), resend);
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

    private AuthResult authenticate(Usuario usuario, Persona persona, String deviceId, String deviceName,
            ClientType clientType) {
        LocalDateTime now = nowUtc();
        sesionRepository.findByUsuarioLoginAndDeviceIdAndFechaRevocacionIsNull(usuario.getLogin(), deviceId)
                .ifPresent(previous -> {
                    previous.revoke(now, RevocationReason.REPLACED_BY_NEW_LOGIN);
                    sesionRepository.saveAndFlush(previous);
                });
        UUID sid = UUID.randomUUID();
        RefreshTokenService.GeneratedRefreshToken refreshToken = refreshTokenService.generate(sid);
        SesionUsuario sesion = new SesionUsuario(sid, usuario, refreshToken.hash(), deviceId, deviceName,
                clientType, now, now.plusDays(jwtProperties.refreshTokenExpirationDays()));
        sesionRepository.save(sesion);
        usuario.setUltimoAcceso(now);
        usuarioRepository.save(usuario);
        return result(usuario, persona, sesion, refreshToken.value());
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private boolean isActivo(Short estado) {
        return Short.valueOf(ACTIVO).equals(estado);
    }
}
