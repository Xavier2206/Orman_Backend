package com.orman.backend.auth.controller;

import com.orman.backend.auth.config.CorsProperties;
import com.orman.backend.auth.config.JwtProperties;
import com.orman.backend.auth.config.RefreshCookieProperties;
import com.orman.backend.auth.config.SecurityConfig;
import com.orman.backend.auth.dto.response.LoginResponse;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.service.AuthContextService;
import com.orman.backend.auth.service.AuthResult;
import com.orman.backend.auth.service.AuthService;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import com.orman.backend.common.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CsrfBootstrapWebMvcTest {

    private static final String CSRF_URL = "/api/v1/auth/csrf";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String FRONTEND_ORIGIN = "https://frontend.example.test";
    private static final UUID SID = UUID.fromString("8acbc3d8-d7a8-49d1-a1b1-35cecc45ad21");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private AuthContextService authContextService;
    @MockitoBean private SessionService sessionService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserAuthorityService userAuthorityService;
    @MockitoBean private JwtProperties jwtProperties;
    @MockitoBean private RefreshCookieProperties cookieProperties;
    @MockitoBean private CorsProperties corsProperties;

    @BeforeEach
    void configureSecurityProperties() {
        when(cookieProperties.refreshName()).thenReturn("orman_refresh");
        when(cookieProperties.sameSite()).thenReturn("None");
        when(cookieProperties.secure()).thenReturn(true);
        when(jwtProperties.refreshTokenExpirationDays()).thenReturn(30L);
        when(corsProperties.allowedOrigins()).thenReturn(List.of(FRONTEND_ORIGIN));
    }

    @Test
    void bootstrapsCsrfWithoutAccessTokenAndExposesItToAllowedOrigin() throws Exception {
        mockMvc.perform(get(CSRF_URL).header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-XSRF-TOKEN"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-XSRF-TOKEN"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void corsPreflightAllowsCredentialedRefreshAndXsrfHeader() throws Exception {
        mockMvc.perform(options(REFRESH_URL)
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsString("X-XSRF-TOKEN")));
    }

    @Test
    void cookieRefreshRequiresXsrfHeaderAndSucceedsWithBootstrappedToken() throws Exception {
        var bootstrap = mockMvc.perform(get(CSRF_URL))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        String csrfToken = bootstrap.getResponse().getHeader("X-XSRF-TOKEN");
        Cookie refreshCookie = new Cookie("orman_refresh", "web-refresh-token");
        when(authService.refresh("web-refresh-token", ClientType.WEB)).thenReturn(webAuthResult());

        mockMvc.perform(post(REFRESH_URL).cookie(refreshCookie, csrfCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(REFRESH_URL).cookie(refreshCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", "incorrect-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(REFRESH_URL).cookie(refreshCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("orman_refresh"))
                .andExpect(cookie().httpOnly("orman_refresh", true));

        verify(authService).refresh("web-refresh-token", ClientType.WEB);
    }

    @Test
    void loginStillReturnsXsrfHeaderAndHttpOnlyRefreshCookie() throws Exception {
        when(authService.login(any())).thenReturn(webAuthResult());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"usuario.demo","password":"clave-ficticia","deviceId":"browser",
                                 "deviceName":"Chrome","clientType":"WEB"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().exists("orman_refresh"))
                .andExpect(cookie().httpOnly("orman_refresh", true));
    }

    @Test
    void logoutAndLogoutAllRemainBearerAuthenticatedWithoutCsrfRequirement() throws Exception {
        var principal = new AuthenticatedUser("usuario.demo", SID);
        var authenticated = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        Cookie refreshCookie = new Cookie("orman_refresh", "web-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout").with(authentication(authenticated)).cookie(refreshCookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/logout-all").with(authentication(authenticated)).cookie(refreshCookie))
                .andExpect(status().isNoContent());

        verify(sessionService).logout(principal);
        verify(sessionService).logoutAll(principal);
    }

    private AuthResult webAuthResult() {
        LoginResponse response = new LoginResponse("usuario.demo", 7, "access-token", null,
                "Bearer", 900, SID);
        return new AuthResult(response, "rotated-refresh-token", ClientType.WEB);
    }
}
