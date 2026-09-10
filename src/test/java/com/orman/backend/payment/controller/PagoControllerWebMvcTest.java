package com.orman.backend.payment.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.service.PagoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagoController.class)
@Import(GlobalExceptionHandler.class)
class PagoControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PagoService pagoService;

    @Test
    void createsPaymentWithCanonicalLocation() throws Exception {
        when(pagoService.create(any(), any(), any())).thenReturn(response(12));

        mockMvc.perform(post("/api/v1/cuotas/8/pagos").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/pagos/12"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    void returnsProblemDetailForConflictValidationAndInvalidState() throws Exception {
        when(pagoService.create(any(), any(), any())).thenThrow(new ConflictException("Conflicto de prueba."));
        mockMvc.perform(post("/api/v1/cuotas/8/pagos").contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        mockMvc.perform(post("/api/v1/cuotas/8/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"monto\":350.00", "\"monto\":0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/pagos").param("estado", "PAGADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void listsOnlyThroughThePaginatedPaymentContract() throws Exception {
        when(pagoService.list(any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/pagos").param("estado", "PENDIENTE_REVISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codpag").value(12));
    }

    private String validJson() {
        return "{\"monto\":350.00,\"metodo\":\"EFECTIVO\",\"fechaPago\":\"2026-09-10T10:00:00\","
                + "\"idempotencyKey\":\"11111111-1111-1111-1111-111111111111\"}";
    }

    private PagoResponse response(Integer codpag) {
        return new PagoResponse(codpag, 8, null, new BigDecimal("350.00"), "EFECTIVO", null,
                LocalDateTime.of(2026, 9, 10, 10, 0), LocalDateTime.of(2026, 9, 10, 10, 1),
                "PENDIENTE_REVISION", "MANUAL", null, null, null);
    }
}
