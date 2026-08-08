package com.orman.backend.auth.controller;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.config.RefreshCookieProperties;
import com.orman.backend.auth.dto.request.LoginRequest;
import com.orman.backend.auth.dto.request.OtpVerifyRequest;
import com.orman.backend.auth.dto.request.OtpResendRequest;
import com.orman.backend.auth.dto.request.RefreshRequest;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.dto.response.SessionResponse;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.model.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_PATH = "/api/v1/auth";
    private final AuthService authService;
    private final SessionService sessionService;
    private final RefreshCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        CsrfToken csrfToken = (CsrfToken) httpRequest.getAttribute("_csrf");
        String csrfValue = csrfToken == null ? null : csrfToken.getToken();
        return response(authService.login(request), csrfValue);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "${security.cookie.refresh-name}", required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest request) {
        String bodyToken = request == null ? null : request.refreshToken();
        RefreshInput input = resolveRefreshInput(cookieToken, bodyToken);
        return response(authService.refresh(input.token(), input.clientType()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {
        CsrfToken csrfToken = (CsrfToken) httpRequest.getAttribute("_csrf");
        return response(authService.verifyOtp(request), csrfToken == null ? null : csrfToken.getToken());
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody OtpResendRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        sessionService.logout(user);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser user) {
        sessionService.logoutAll(user);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public List<SessionResponse> sessions(@AuthenticationPrincipal AuthenticatedUser user) {
        return sessionService.list(user);
    }

    @DeleteMapping("/sessions/{sid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID sid) {
        sessionService.revoke(user, sid);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<LoginResponse> response(AuthResult result) {
        return response(result, null);
    }

    private ResponseEntity<LoginResponse> response(AuthResult result, String csrfToken) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.clientType() == ClientType.WEB && result.refreshToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString());
        }
        if (csrfToken != null) {
            builder.header("X-XSRF-TOKEN", csrfToken);
        }
        return builder.body(result.response());
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(cookieProperties.refreshName(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(REFRESH_PATH)
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenExpirationDays()))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(cookieProperties.refreshName(), "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(REFRESH_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private RefreshInput resolveRefreshInput(String cookieToken, String bodyToken) {
        boolean hasCookie = StringUtils.hasText(cookieToken);
        boolean hasBody = StringUtils.hasText(bodyToken);
        if (!hasCookie && !hasBody) {
            throw new InvalidRefreshTokenException();
        }
        if (hasCookie && hasBody && !constantTimeEquals(cookieToken, bodyToken)) {
            throw new InvalidRefreshTokenException();
        }
        return hasCookie
                ? new RefreshInput(cookieToken, ClientType.WEB)
                : new RefreshInput(bodyToken, ClientType.MOBILE);
    }

    private boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    private record RefreshInput(String token, ClientType clientType) {
    }
}
