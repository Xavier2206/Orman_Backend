package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.OtpProperties;
import com.orman.backend.auth.service.OtpDigestService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HmacSha256OtpDigestService implements OtpDigestService {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final OtpProperties properties;

    @Override
    public String digest(UUID challengeId, String otp) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(properties.hmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal((challengeId + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA-256 no está disponible.", exception);
        }
    }

    @Override
    public boolean matches(UUID challengeId, String otp, String expectedDigest) {
        if (otp == null || expectedDigest == null) {
            return false;
        }
        return MessageDigest.isEqual(digest(challengeId, otp).getBytes(StandardCharsets.US_ASCII),
                expectedDigest.getBytes(StandardCharsets.US_ASCII));
    }
}
