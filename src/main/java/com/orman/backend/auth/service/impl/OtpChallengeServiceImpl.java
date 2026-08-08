package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.OtpProperties;
import com.orman.backend.auth.entity.OtpChallenge;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.OtpChallengeStatus;
import com.orman.backend.auth.model.OtpPurpose;
import com.orman.backend.auth.repository.OtpChallengeRepository;
import com.orman.backend.auth.service.OtpChallengeService;
import com.orman.backend.auth.service.OtpDigestService;
import com.orman.backend.auth.service.OtpGenerator;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OtpChallengeServiceImpl implements OtpChallengeService {

    private final OtpChallengeRepository challengeRepository;
    private final UsuarioRepository usuarioRepository;
    private final OtpGenerator otpGenerator;
    private final OtpDigestService digestService;
    private final OtpProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public CreatedOtpChallenge createLoginChallenge(String login, ClientType clientType, String ipAddress, String userAgent) {
        usuarioRepository.findByLoginForUpdate(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        challengeRepository.findByLoginAndClientTypeAndPurposeAndStatus(login, clientType, OtpPurpose.LOGIN,
                OtpChallengeStatus.PENDING).ifPresent(previous -> {
                    previous.cancel();
                    challengeRepository.flush();
                });
        LocalDateTime now = now();
        UUID id = UUID.randomUUID();
        String otp = otpGenerator.generate();
        OtpChallenge challenge = new OtpChallenge(id, login, clientType, OtpPurpose.LOGIN,
                digestService.digest(id, otp), now, now.plusSeconds(properties.expirationSeconds()), ipAddress, userAgent);
        return new CreatedOtpChallenge(challengeRepository.saveAndFlush(challenge), otp);
    }

    @Override
    @Transactional
    public OtpVerificationResult verify(UUID challengeId, String otp) {
        OtpChallenge challenge = findForUpdate(challengeId);
        requirePending(challenge);
        LocalDateTime now = now();
        if (!challenge.getExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("El challenge OTP ha expirado.");
        }
        if (!isOtpFormatValid(otp)) {
            throw new BusinessRuleException("El código OTP debe contener exactamente seis dígitos.");
        }
        if (digestService.matches(challengeId, otp, challenge.getOtpDigest())) {
            challenge.verify(now);
            return OtpVerificationResult.VERIFIED;
        }
        return challenge.registerFailedAttempt(properties.maxAttempts())
                ? OtpVerificationResult.LOCKED : OtpVerificationResult.INVALID_CODE;
    }

    @Override
    @Transactional
    public PreparedOtpResend prepareResend(UUID challengeId) {
        OtpChallenge challenge = findForUpdate(challengeId);
        requirePending(challenge);
        LocalDateTime now = now();
        if (!challenge.getExpiresAt().isAfter(now)) {
            throw new BusinessRuleException("El challenge OTP ha expirado.");
        }
        if (challenge.getResendCount() >= properties.maxResends()) {
            throw new BusinessRuleException("El challenge OTP alcanzó el máximo de reenvíos.");
        }
        if (challenge.getLastSentAt() != null
                && challenge.getLastSentAt().plusSeconds(properties.resendCooldownSeconds()).isAfter(now)) {
            throw new BusinessRuleException("El reenvío OTP todavía está en cooldown.");
        }
        String otp = otpGenerator.generate();
        return new PreparedOtpResend(challenge, otp);
    }

    @Override
    @Transactional
    public void confirmResend(UUID challengeId, PreparedOtpResend resend) {
        OtpChallenge challenge = findForUpdate(challengeId);
        requirePending(challenge);
        if (!challenge.getId().equals(resend.challenge().getId())) {
            throw new BusinessRuleException("El challenge OTP no es válido.");
        }
        LocalDateTime now = now();
        challenge.prepareResend(digestService.digest(challengeId, resend.otp()),
                now.plusSeconds(properties.expirationSeconds()));
        challenge.markSent(now);
    }

    @Override
    @Transactional
    public void markSent(UUID challengeId) {
        OtpChallenge challenge = findForUpdate(challengeId);
        requirePending(challenge);
        challenge.markSent(now());
    }

    @Override
    @Transactional
    public void cancel(UUID challengeId) {
        OtpChallenge challenge = findForUpdate(challengeId);
        if (challenge.getStatus() == OtpChallengeStatus.PENDING) {
            challenge.cancel();
        }
    }

    private OtpChallenge findForUpdate(UUID id) {
        return challengeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge OTP no encontrado."));
    }

    private void requirePending(OtpChallenge challenge) {
        if (challenge.getStatus() != OtpChallengeStatus.PENDING) {
            throw new BusinessRuleException("El challenge OTP no está pendiente.");
        }
    }

    private boolean isOtpFormatValid(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
