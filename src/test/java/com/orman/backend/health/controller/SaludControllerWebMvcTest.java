package com.orman.backend.health.controller;

import com.orman.backend.auth.config.CorsProperties;
import com.orman.backend.auth.config.RefreshCookieProperties;
import com.orman.backend.auth.config.SecurityConfig;
import com.orman.backend.auth.service.JwtService;
import com.orman.backend.auth.service.SessionService;
import com.orman.backend.auth.service.UserAuthorityService;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.health.service.SaludService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SaludController.class, properties = "security.cors.allowed-origins=https://frontend.example.test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SaludControllerWebMvcTest.WebSecurityTestConfiguration.class})
class SaludControllerWebMvcTest {

    private static final String SALUD_URL = "/api/v1/salud";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SaludService saludService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private SessionService sessionService;
    @MockitoBean private UserAuthorityService userAuthorityService;
    @MockitoBean private RefreshCookieProperties cookieProperties;

    @Test
    void respondeSinTokenYCompruebaLaBaseDeDatos() throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(true);

        mockMvc.perform(get(SALUD_URL))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"estado":"UP","baseDatos":"UP"}
                        """));
    }

    @Test
    void noIntentaValidarUnTokenEnLaRutaPublica() throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(true);

        mockMvc.perform(get(SALUD_URL).header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isOk());

        verifyNoInteractions(jwtService, sessionService, userAuthorityService);
    }

    @Test
    void responde503SinExponerDetallesCuandoPostgresNoEstaDisponible() throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(false);

        mockMvc.perform(get(SALUD_URL))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("""
                        {"estado":"DOWN","baseDatos":"DOWN"}
                        """));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(CorsProperties.class)
    static class WebSecurityTestConfiguration {
    }
}
