package com.orman.backend.auth.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.exception.ExpiredJwtException;
import com.orman.backend.auth.service.JwtService;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NimbusJwtService implements JwtService {

    private final JwtProperties properties;
    private final Clock clock;

    @Override
    public String generateAccessToken(String login, UUID sid) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(properties.accessTokenExpirationSeconds());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(login)
                .claim("sid", sid.toString())
                .issuer(properties.issuer())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new MACSigner(secretBytes()));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("No fue posible firmar el access token.", exception);
        }
    }

    @Override
    public JwtClaims validateAndExtract(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secretBytes()))) {
                throw new InvalidJwtException();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            String subject = required(claims.getSubject());
            String issuer = required(claims.getIssuer());
            Date issuedAt = required(claims.getIssueTime());
            Date expiresAt = required(claims.getExpirationTime());
            UUID sid = UUID.fromString(required(claims.getStringClaim("sid")));
            if (!properties.issuer().equals(issuer)) {
                throw new InvalidJwtException();
            }
            if (!expiresAt.toInstant().isAfter(clock.instant())) {
                throw new ExpiredJwtException();
            }
            return new JwtClaims(subject, sid, issuer, issuedAt.toInstant(), expiresAt.toInstant());
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new InvalidJwtException();
        }
    }

    private byte[] secretBytes() {
        return properties.secret().getBytes(StandardCharsets.UTF_8);
    }

    private <T> T required(T value) {
        if (value == null || value instanceof String text && text.isBlank()) {
            throw new InvalidJwtException();
        }
        return value;
    }
}
