package com.orman.backend.auth.controller;

import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.config.RefreshCookieProperties;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.exception.InvalidCredentialsException;
import com.orman.backend.auth.exception.InvalidRefreshTokenException;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.common.error.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerWebMvcTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final UUID SID = UUID.fromString("8acbc3d8-d7a8-49d1-a1b1-35cecc45ad21");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private SessionService sessionService;
    @MockitoBean private JwtProperties jwtProperties;
    @MockitoBean private RefreshCookieProperties cookieProperties;

    @BeforeEach
    void configureCookie() {
        when(cookieProperties.refreshName()).thenReturn("orman_refresh");
        when(cookieProperties.sameSite()).thenReturn("Lax");
        when(cookieProperties.secure()).thenReturn(false);
        when(jwtProperties.refreshTokenExpirationDays()).thenReturn(30L);
    }

    @Test
    void returnsMobileTokensInJsonWithoutSensitiveFields() throws Exception {
        when(authService.login(any())).thenReturn(result(ClientType.MOBILE, "refresh-mobile"));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest("mobile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("usuario.demo"))
                .andExpect(jsonPath("$.codper").value(7))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-mobile"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.sid").value(SID.toString()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$.persona").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    void returnsWebTokenAndSecureCookieContractWithoutRefreshInJson() throws Exception {
        when(authService.login(any())).thenReturn(result(ClientType.WEB, null));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest("web")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("orman_refresh", "rotated-refresh"))
                .andExpect(cookie().httpOnly("orman_refresh", true))
                .andExpect(cookie().secure("orman_refresh", false))
                .andExpect(cookie().path("orman_refresh", "/api/v1/auth"))
                .andExpect(cookie().maxAge("orman_refresh", 2_592_000))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")));
    }

    @Test
    void refreshesMobileFromBodyAndWebFromCookie() throws Exception {
        when(authService.refresh("mobile-token", ClientType.MOBILE)).thenReturn(result(ClientType.MOBILE, "new-mobile"));
        when(authService.refresh("web-token", ClientType.WEB)).thenReturn(result(ClientType.WEB, null));

        mockMvc.perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"mobile-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-mobile"));

        mockMvc.perform(post(REFRESH_URL).cookie(new jakarta.servlet.http.Cookie("orman_refresh", "web-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("orman_refresh", "rotated-refresh"));
    }

    @Test
    void rejectsMissingOrContradictoryRefreshWithSafeProblemDetail() throws Exception {
        mockMvc.perform(post(REFRESH_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.detail").value("La sesión no es válida o ha expirado."));

        mockMvc.perform(post(REFRESH_URL).cookie(new jakarta.servlet.http.Cookie("orman_refresh", "cookie-token"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"body-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void returnsSafeAuthenticationErrorsAndValidatesNewFields() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(validRequest("MOBILE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"usuario.demo\",\"password\":\"clave-ficticia\",\"deviceId\":\" \",\"deviceName\":\"x\",\"clientType\":\"MOBILE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"usuario.demo\",\"password\":\"clave-ficticia\",\"deviceId\":\"x\",\"deviceName\":\"x\",\"clientType\":\"DESKTOP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private AuthResult result(ClientType type, String responseRefresh) {
        LoginResponse response = new LoginResponse("usuario.demo", 7, "access-token", responseRefresh,
                "Bearer", 900, SID);
        return new AuthResult(response, "rotated-refresh", type);
    }

    private String validRequest(String clientType) {
        return "{\"login\":\"usuario.demo\",\"password\":\"clave-ficticia\","
                + "\"deviceId\":\" device-1 \",\"deviceName\":\" Android \",\"clientType\":\""
                + clientType + "\"}";
    }
}
