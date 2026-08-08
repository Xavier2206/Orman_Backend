package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.service.OtpGenerator;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecureRandomOtpGenerator implements OtpGenerator {

    private static final int OTP_BOUND = 1_000_000;
    private final SecureRandom secureRandom;

    @Override
    public String generate() {
        return "%06d".formatted(secureRandom.nextInt(OTP_BOUND));
    }
}
