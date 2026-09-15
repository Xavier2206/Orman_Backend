package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.service.ContratoService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(ContratoController.class)
@Import(GlobalExceptionHandler.class)
class ContratoControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContratoService contratoService;

    @Test
    void createsContractWithCanonicalLocation() throws Exception {
        when(contratoService.create(any(), any(), any())).thenReturn(response(12));

        mockMvc.perform(post("/api/v1/unidades/8/contratos").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/contratos/12"))
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.moneda").value("BOB"));
    }

    @Test
    void returnsProblemDetailForConflictValidationAndInvalidState() throws Exception {
        when(contratoService.create(any(), any(), any())).thenThrow(new ConflictException("Conflicto de prueba."));
        mockMvc.perform(post("/api/v1/unidades/8/contratos").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        mockMvc.perform(post("/api/v1/unidades/8/contratos").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"montoMensual\":2500.00", "\"montoMensual\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/unidades/8/contratos").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"montoMensual\":2500.00", "\"montoMensual\":2500.001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/contratos").param("estado", "PAGADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void listsOnlyThroughThePaginatedContract() throws Exception {
        when(contratoService.list(any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/contratos").param("estado", "PROGRAMADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codcon").value(12));
    }

    private String validJson() {
        return "{\"codperInquilino\":4,\"fechaInicio\":\"2026-09-01\",\"fechaFin\":\"2027-09-01\","
                + "\"montoMensual\":2500.00,\"garantia\":2500.00}";
    }

    private ContratoResponse response(Integer codcon) {
        return new ContratoResponse(codcon, 8, 4, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1),
                new BigDecimal("2500.00"), "BOB", new BigDecimal("2500.00"), "VIGENTE",
                java.time.LocalDateTime.of(2026, 9, 1, 12, 0), null, null);
    }
}
