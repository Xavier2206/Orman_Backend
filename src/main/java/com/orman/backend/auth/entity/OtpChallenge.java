package com.orman.backend.auth.entity;

import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.OtpChallengeStatus;
import com.orman.backend.auth.model.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "otp_challenges")
@Getter
@NoArgsConstructor
@DynamicInsert
public class OtpChallenge {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "login", nullable = false, updatable = false, length = 30)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, updatable = false, length = 10)
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 40)
    private OtpPurpose purpose;

    @Column(name = "otp_digest", nullable = false, length = 64)
    private String otpDigest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpChallengeStatus status;

    @Column(name = "attempts", nullable = false)
    private Short attempts;

    @Column(name = "resend_count", nullable = false)
    private Short resendCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    public OtpChallenge(UUID id, String login, ClientType clientType, OtpPurpose purpose, String otpDigest,
            OtpChallengeStatus status, LocalDateTime expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.login = Objects.requireNonNull(login);
        this.clientType = Objects.requireNonNull(clientType);
        this.purpose = Objects.requireNonNull(purpose);
        this.otpDigest = Objects.requireNonNull(otpDigest);
        this.status = Objects.requireNonNull(status);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public OtpChallenge(UUID id, String login, ClientType clientType, OtpPurpose purpose, String otpDigest,
            LocalDateTime createdAt, LocalDateTime expiresAt, String ipAddress, String userAgent) {
        this(id, login, clientType, purpose, otpDigest, OtpChallengeStatus.PENDING, expiresAt);
        this.attempts = 0;
        this.resendCount = 0;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public void cancel() {
        requirePending();
        status = OtpChallengeStatus.CANCELLED;
    }

    public void verify(LocalDateTime now) {
        requirePending();
        status = OtpChallengeStatus.VERIFIED;
        verifiedAt = Objects.requireNonNull(now);
    }

    public boolean registerFailedAttempt(int maxAttempts) {
        requirePending();
        attempts++;
        if (attempts >= maxAttempts) {
            status = OtpChallengeStatus.LOCKED;
            return true;
        }
        return false;
    }

    public void prepareResend(String digest, LocalDateTime expiration) {
        requirePending();
        otpDigest = Objects.requireNonNull(digest);
        expiresAt = Objects.requireNonNull(expiration);
        resendCount++;
    }

    public void markSent(LocalDateTime now) {
        requirePending();
        lastSentAt = Objects.requireNonNull(now);
    }

    private void requirePending() {
        if (status != OtpChallengeStatus.PENDING) {
            throw new IllegalStateException("El challenge OTP no está pendiente.");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        OtpChallenge challenge = (OtpChallenge) other;
        return id != null && Objects.equals(id, challenge.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
