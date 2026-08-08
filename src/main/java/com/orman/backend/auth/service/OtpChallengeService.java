package com.orman.backend.auth.service;

import com.orman.backend.auth.entity.OtpChallenge;
import com.orman.backend.auth.model.ClientType;
import java.time.LocalDateTime;
import java.util.UUID;

public interface OtpChallengeService {

    CreatedOtpChallenge createLoginChallenge(String login, ClientType clientType, String ipAddress, String userAgent);

    OtpVerificationResult verify(UUID challengeId, String otp);

    PreparedOtpResend prepareResend(UUID challengeId);

    void confirmResend(UUID challengeId, PreparedOtpResend resend);

    void markSent(UUID challengeId);

    void cancel(UUID challengeId);

    record CreatedOtpChallenge(OtpChallenge challenge, String otp) {
        @Override public String toString() { return "CreatedOtpChallenge[challenge=" + challenge.getId() + ", otp=<redacted>]"; }
    }

    record PreparedOtpResend(OtpChallenge challenge, String otp) {
        @Override public String toString() { return "PreparedOtpResend[challenge=" + challenge.getId() + ", otp=<redacted>]"; }
    }

    enum OtpVerificationResult { VERIFIED, INVALID_CODE, LOCKED }
}
