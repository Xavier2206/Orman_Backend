CREATE TABLE otp_challenges (
    id UUID NOT NULL,
    login VARCHAR(30) NOT NULL,
    client_type VARCHAR(10) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    otp_digest VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    resend_count SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_sent_at TIMESTAMP,
    verified_at TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,

    CONSTRAINT pk_otp_challenges PRIMARY KEY (id),
    CONSTRAINT fk_otp_challenges_login
        FOREIGN KEY (login) REFERENCES usuarios (login),
    CONSTRAINT ck_otp_challenges_client_type
        CHECK (client_type IN ('WEB', 'MOBILE')),
    CONSTRAINT ck_otp_challenges_purpose
        CHECK (purpose IN ('LOGIN')),
    CONSTRAINT ck_otp_challenges_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'LOCKED', 'CANCELLED')),
    CONSTRAINT ck_otp_challenges_attempts
        CHECK (attempts >= 0),
    CONSTRAINT ck_otp_challenges_resend_count
        CHECK (resend_count >= 0),
    CONSTRAINT ck_otp_challenges_expiration
        CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX uk_otp_challenges_login_client_type_purpose_pending
    ON otp_challenges (login, client_type, purpose)
    WHERE status = 'PENDING';
