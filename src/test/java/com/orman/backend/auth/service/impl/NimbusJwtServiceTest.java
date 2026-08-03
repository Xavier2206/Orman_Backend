package com.orman.backend.auth.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.exception.InvalidJwtException;
import com.orman.backend.auth.service.JwtService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusJwtServiceTest {

    private static final String SECRET = "test-secret-64-bytes-long-for-hs256-and-hs512-validation-00000000";
    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");
    private static final UUID SID = UUID.fromString("12aa107e-48b4-40f8-b66f-4c9f1906623d");

    @Test
    void generatesAndValidatesRequiredClaimsWithFifteenMinuteLifetime() {
        NimbusJwtService service = service("orman-backend", NOW);

        String token = service.generateAccessToken("usuario.demo", SID);
        JwtService.JwtClaims claims = service.validateAndExtract(token);

        assertThat(claims.subject()).isEqualTo("usuario.demo");
        assertThat(claims.sid()).isEqualTo(SID);
        assertThat(claims.issuer()).isEqualTo("orman-backend");
        assertThat(claims.issuedAt()).isEqualTo(NOW);
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void rejectsTamperingWrongIssuerAndExpiration() {
        NimbusJwtService issuer = service("orman-backend", NOW);
        String token = issuer.generateAccessToken("usuario.demo", SID);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> issuer.validateAndExtract(tampered)).isInstanceOf(InvalidJwtException.class);
        assertThatThrownBy(() -> service("other-issuer", NOW).validateAndExtract(token))
                .isInstanceOf(InvalidJwtException.class);
        assertThatThrownBy(() -> service("orman-backend", NOW.plusSeconds(901)).validateAndExtract(token))
                .isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void rejectsNoneDifferentAlgorithmAndMissingClaims() throws JOSEException {
        String none = new com.nimbusds.jwt.PlainJWT(new JWTClaimsSet.Builder()
                .subject("usuario.demo").issuer("orman-backend").claim("sid", SID.toString())
                .issueTime(Date.from(NOW)).expirationTime(Date.from(NOW.plusSeconds(900))).build()).serialize();
        String hs512 = signed(new JWTClaimsSet.Builder().subject("usuario.demo").issuer("orman-backend")
                .claim("sid", SID.toString()).issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plusSeconds(900))).build(), JWSAlgorithm.HS512);
        String missingSid = signed(new JWTClaimsSet.Builder().subject("usuario.demo").issuer("orman-backend")
                .issueTime(Date.from(NOW)).expirationTime(Date.from(NOW.plusSeconds(900))).build(), JWSAlgorithm.HS256);

        NimbusJwtService service = service("orman-backend", NOW);
        assertThatThrownBy(() -> service.validateAndExtract(none)).isInstanceOf(InvalidJwtException.class);
        assertThatThrownBy(() -> service.validateAndExtract(hs512)).isInstanceOf(InvalidJwtException.class);
        assertThatThrownBy(() -> service.validateAndExtract(missingSid)).isInstanceOf(InvalidJwtException.class);
    }

    private NimbusJwtService service(String issuer, Instant instant) {
        return new NimbusJwtService(new JwtProperties(SECRET, issuer, 15, 30),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private String signed(JWTClaimsSet claims, JWSAlgorithm algorithm) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
