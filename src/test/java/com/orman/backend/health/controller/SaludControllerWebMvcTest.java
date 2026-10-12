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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
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

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD"})
    void noIntentaValidarUnTokenEnLaRutaPublica(String method) throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(true);

        mockMvc.perform(request(HttpMethod.valueOf(method), SALUD_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
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

    @Test
    void headSinTokenCompruebaLaBaseDeDatos() throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(true);

        mockMvc.perform(head(SALUD_URL))
                .andExpect(status().isOk());

        verify(saludService).comprobarBaseDatos();
        verifyNoInteractions(jwtService, sessionService, userAuthorityService);
    }

    @Test
    void headConBaseDeDatosNoDisponibleResponde503() throws Exception {
        when(saludService.comprobarBaseDatos()).thenReturn(false);

        mockMvc.perform(head(SALUD_URL))
                .andExpect(status().isServiceUnavailable());

        verify(saludService).comprobarBaseDatos();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD"})
    void otrosRecursosSiguenProtegidosSinToken(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/v1/auth/sessions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(request(HttpMethod.valueOf(method), SALUD_URL + "/privado"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(saludService, jwtService, sessionService, userAuthorityService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void otrosMetodosDeSaludSiguenProtegidosSinToken(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), SALUD_URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(saludService, jwtService, sessionService, userAuthorityService);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableConfigurationProperties(CorsProperties.class)
    static class WebSecurityTestConfiguration {
    }
}
