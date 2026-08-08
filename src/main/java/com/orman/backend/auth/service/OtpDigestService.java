package com.orman.backend.auth.service;

import java.util.UUID;

public interface OtpDigestService {

    String digest(UUID challengeId, String otp);

    boolean matches(UUID challengeId, String otp, String expectedDigest);
}
