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
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpChallengeServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneOffset.UTC);
    private final OtpChallengeRepository repository = mock(OtpChallengeRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final OtpGenerator generator = mock(OtpGenerator.class);
    private final OtpDigestService digestService = mock(OtpDigestService.class);
    private final OtpChallengeService service = new OtpChallengeServiceImpl(repository, usuarioRepository, generator,
            digestService, new OtpProperties("test-only-otp-hmac-secret-at-least-32-bytes", 300, 5, 60, 3), CLOCK);

    @Test
    void createsChallengeCancellingPreviousPendingAndNeverExposingOtpInEntity() {
        Usuario usuario = new Usuario();
        usuario.setLogin("otp.service");
        OtpChallenge previous = challenge("old", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK).plusMinutes(5));
        when(usuarioRepository.findByLoginForUpdate("otp.service")).thenReturn(Optional.of(usuario));
        when(repository.findByLoginAndClientTypeAndPurposeAndStatus("otp.service", ClientType.WEB, OtpPurpose.LOGIN,
                OtpChallengeStatus.PENDING)).thenReturn(Optional.of(previous));
        when(generator.generate()).thenReturn("004812");
        when(digestService.digest(any(UUID.class), anyString())).thenReturn("A".repeat(64));
        when(repository.saveAndFlush(any(OtpChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpChallengeService.CreatedOtpChallenge created = service.createLoginChallenge("otp.service", ClientType.WEB,
                "192.0.2.1", "Browser");

        assertThat(previous.getStatus()).isEqualTo(OtpChallengeStatus.CANCELLED);
        assertThat(created.challenge().getStatus()).isEqualTo(OtpChallengeStatus.PENDING);
        assertThat(created.challenge().getAttempts()).isZero();
        assertThat(created.challenge().getResendCount()).isZero();
        assertThat(created.challenge().getExpiresAt()).isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(5));
        assertThat(created.challenge().getOtpDigest()).isNotEqualTo(created.otp());
        assertThat(created.toString()).doesNotContain(created.otp());
    }

    @Test
    void verifiesCorrectOtpAndLocksOnFifthIncorrectAttempt() {
        OtpChallenge challenge = challenge("verify", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK).plusMinutes(5));
        when(repository.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        when(digestService.matches(challenge.getId(), "004812", challenge.getOtpDigest())).thenReturn(true);
        assertThat(service.verify(challenge.getId(), "004812")).isEqualTo(OtpChallengeService.OtpVerificationResult.VERIFIED);
        assertThat(challenge.getStatus()).isEqualTo(OtpChallengeStatus.VERIFIED);
        assertThat(challenge.getVerifiedAt()).isEqualTo(LocalDateTime.now(CLOCK));

        OtpChallenge incorrect = challenge("incorrect", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK).plusMinutes(5));
        when(repository.findByIdForUpdate(incorrect.getId())).thenReturn(Optional.of(incorrect));
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(service.verify(incorrect.getId(), "111111")).isEqualTo(
                    attempt == 5 ? OtpChallengeService.OtpVerificationResult.LOCKED
                            : OtpChallengeService.OtpVerificationResult.INVALID_CODE);
        }
        assertThat(incorrect.getAttempts()).isEqualTo((short) 5);
        assertThat(incorrect.getStatus()).isEqualTo(OtpChallengeStatus.LOCKED);
        assertThatThrownBy(() -> service.verify(incorrect.getId(), "004812")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsExpiredMalformedAndNonPendingChallenges() {
        OtpChallenge expired = challenge("expired", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK));
        when(repository.findByIdForUpdate(expired.getId())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verify(expired.getId(), "004812")).isInstanceOf(RuntimeException.class);
        assertThat(expired.getAttempts()).isZero();

        OtpChallenge pending = challenge("format", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK).plusMinutes(5));
        when(repository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.verify(pending.getId(), "1234a")).isInstanceOf(RuntimeException.class);
        pending.cancel();
        assertThatThrownBy(() -> service.verify(pending.getId(), "004812")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void resendReplacesDigestWithoutResettingAttemptsAndUsesRealSentTimeForCooldown() {
        OtpChallenge challenge = challenge("resend", OtpChallengeStatus.PENDING, LocalDateTime.now(CLOCK).plusMinutes(5));
        challenge.registerFailedAttempt(5);
        when(repository.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        when(generator.generate()).thenReturn("999999");
        when(digestService.digest(challenge.getId(), "999999")).thenReturn("B".repeat(64));

        OtpChallengeService.PreparedOtpResend resend = service.prepareResend(challenge.getId());
        assertThat(resend.otp()).isEqualTo("999999");
        assertThat(challenge.getOtpDigest()).isEqualTo("A".repeat(64));
        assertThat(challenge.getAttempts()).isEqualTo((short) 1);
        assertThat(challenge.getResendCount()).isZero();
        service.confirmResend(challenge.getId(), resend);
        assertThat(challenge.getOtpDigest()).isEqualTo("B".repeat(64));
        assertThat(challenge.getResendCount()).isEqualTo((short) 1);
        assertThat(challenge.getLastSentAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThatThrownBy(() -> service.prepareResend(challenge.getId())).isInstanceOf(RuntimeException.class);
    }

    private OtpChallenge challenge(String marker, OtpChallengeStatus status, LocalDateTime expiration) {
        OtpChallenge challenge = new OtpChallenge(UUID.nameUUIDFromBytes(marker.getBytes()), "otp.service", ClientType.WEB,
                OtpPurpose.LOGIN, "A".repeat(64), LocalDateTime.now(CLOCK), expiration, null, null);
        if (status == OtpChallengeStatus.CANCELLED) challenge.cancel();
        return challenge;
    }
}
