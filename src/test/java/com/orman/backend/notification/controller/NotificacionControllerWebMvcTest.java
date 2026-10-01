package com.orman.backend.notification.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.dto.response.NotificacionResumenResponse;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.notification.service.NotificacionService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({NotificacionController.class, CuotaNotificacionController.class})
@Import(GlobalExceptionHandler.class)
class NotificacionControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificacionService notificacionService;
    @MockitoBean private NotificacionGeneracionService notificacionGeneracionService;

    @Test
    @WithMockUser(roles = "INQUILINO")
    void exposesTheNotificationQueriesAndReadAction() throws Exception {
        when(notificacionService.list(any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(response(14)), 0, 20, 1, 1, true, true));
        when(notificacionService.get(any(), any())).thenReturn(response(14));
        when(notificacionService.summary(any())).thenReturn(new NotificacionResumenResponse(1));
        when(notificacionService.markAsRead(any(), any())).thenReturn(response(14));

        mockMvc.perform(get("/api/v1/notificaciones").param("tipo", "CUOTA_VENCIDA").param("leida", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codnot").value(14))
                .andExpect(jsonPath("$.content[0].fechaCreacion").value("2026-09-10T06:00:00-04:00"))
                .andExpect(jsonPath("$.content[0].fechaLectura").value("2026-09-10T06:01:00-04:00"));
        mockMvc.perform(get("/api/v1/notificaciones/14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenciaTipo").value("CUOTA"))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-09-10T06:00:00-04:00"))
                .andExpect(jsonPath("$.fechaLectura").value("2026-09-10T06:01:00-04:00"));
        mockMvc.perform(get("/api/v1/notificaciones/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(1));
        mockMvc.perform(patch("/api/v1/notificaciones/14/leer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    @WithMockUser(roles = "PROPIETARIO")
    void validatesTypeAndCreatesManualPendingPaymentReminder() throws Exception {
        when(notificacionGeneracionService.notifyPendingPayment(any(), any())).thenReturn(
                new NotificacionResponse(15L, "CUOTA_VENCIDA", "Pago pendiente",
                        "Tienes pendiente el pago de Bs 2.500,00 correspondiente a septiembre de 2026.",
                        "CUOTA", 8, OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, ZoneOffset.ofHours(-4)), false,
                        null));

        mockMvc.perform(get("/api/v1/notificaciones").param("tipo", "INVALIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/v1/cuotas/8/notificar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codnot").value(15))
                .andExpect(jsonPath("$.titulo").value("Pago pendiente"))
                .andExpect(jsonPath("$.referenciaTipo").value("CUOTA"))
                .andExpect(jsonPath("$.referenciaId").value(8))
                .andExpect(jsonPath("$.mensaje").value(
                        "Tienes pendiente el pago de Bs 2.500,00 correspondiente a septiembre de 2026."));
    }

    private NotificacionResponse response(long codnot) {
        return new NotificacionResponse(codnot, "CUOTA_VENCIDA", "Cuota vencida",
                "La cuota se encuentra vencida.", "CUOTA", 8,
                OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, ZoneOffset.ofHours(-4)), true,
                OffsetDateTime.of(2026, 9, 10, 6, 1, 0, 0, ZoneOffset.ofHours(-4)));
    }
}
