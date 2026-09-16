package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.dto.response.ContratoResumenResponse;
import com.orman.backend.contract.entity.ContratoEstado;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
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
        when(contratoService.list(any(), any(), any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/contratos")
                        .param("q", "Juan")
                        .param("codprop", "10")
                        .param("coduni", "25")
                        .param("estado", "VIGENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codcon").value(12));

        verify(contratoService).list(eq("Juan"), eq(10), eq(25), eq(ContratoEstado.VIGENTE), any(), any());

        when(contratoService.list(isNull(String.class), isNull(Integer.class), isNull(Integer.class),
                isNull(ContratoEstado.class), any(), any()))
                .thenReturn(new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codcon").value(12));

        verify(contratoService).list(isNull(String.class), isNull(Integer.class), isNull(Integer.class),
                isNull(ContratoEstado.class), any(), any());
    }

    @Test
    void returnsContractSummaryForDashboardCards() throws Exception {
        when(contratoService.resumen(any())).thenReturn(new ContratoResumenResponse(4, 2, 6, 1));

        mockMvc.perform(get("/api/v1/contratos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigentes").value(4))
                .andExpect(jsonPath("$.programados").value(2))
                .andExpect(jsonPath("$.finalizados").value(6))
                .andExpect(jsonPath("$.rescindidos").value(1));

        verify(contratoService).resumen(any());
    }

    @Test
    void listReturnsEnrichedContractResponseFields() throws Exception {
        ContratoResponse enriched = new ContratoResponse(15, 8, 4, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1),
                new BigDecimal("2500.00"), "BOB", new BigDecimal("2500.00"), "VIGENTE",
                java.time.LocalDateTime.of(2026, 9, 1, 12, 0), null, null,
                new com.orman.backend.contract.dto.response.ContratoInquilinoResponse(4, "Carlos Mendoza", "4892014 SC"),
                new com.orman.backend.contract.dto.response.ContratoUnidadResponse(8, "Dpto. 2A", "Residencial", "Depto 2", 2),
                new com.orman.backend.contract.dto.response.ContratoPropiedadResponse(3, "Edificio Central"),
                new com.orman.backend.contract.dto.response.ContratoCuotasResumenResponse(12, 8, 4, new BigDecimal("6000.00")));

        when(contratoService.list(isNull(String.class), isNull(Integer.class), isNull(Integer.class),
                isNull(ContratoEstado.class), any(), any()))
                .thenReturn(new PageResponse<>(List.of(enriched), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codcon").value(15))
                .andExpect(jsonPath("$.content[0].inquilino.nombreCompleto").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.content[0].inquilino.ci").value("4892014 SC"))
                .andExpect(jsonPath("$.content[0].unidad.nombre").value("Dpto. 2A"))
                .andExpect(jsonPath("$.content[0].unidad.piso").value(2))
                .andExpect(jsonPath("$.content[0].propiedad.nombre").value("Edificio Central"))
                .andExpect(jsonPath("$.content[0].cuotas.totalCuotas").value(12))
                .andExpect(jsonPath("$.content[0].cuotas.cuotasPagadas").value(8))
                .andExpect(jsonPath("$.content[0].cuotas.cuotasPendientes").value(4))
                .andExpect(jsonPath("$.content[0].cuotas.saldoPendiente").value(6000.00))
                .andExpect(jsonPath("$.content[0].archivos").doesNotExist());
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
