package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.contract.dto.response.InquilinoContratoResponse;
import com.orman.backend.contract.dto.response.InquilinoCuotaResponse;
import com.orman.backend.contract.service.InquilinoPortalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquilinoPortalController.class)
@Import(GlobalExceptionHandler.class)
class InquilinoPortalControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InquilinoPortalService portalService;

    @Test
    @WithMockUser(roles = "INQUILINO")
    void exposesOwnContractAndQuotaPortalRoutes() throws Exception {
        when(portalService.listContracts(any(), any())).thenReturn(new PageResponse<>(
                List.of(contractResponse()), 0, 20, 1, 1, true, true));
        when(portalService.getContract(eq(8), any())).thenReturn(contractResponse());
        when(portalService.listQuotas(eq(8), any())).thenReturn(List.of(quotaResponse()));
        when(portalService.getQuota(eq(22), any())).thenReturn(quotaResponse());

        mockMvc.perform(get("/api/v1/inquilino/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].estado").value("VIGENTE"))
                .andExpect(jsonPath("$.content[0].nombrePropiedad").value("Edificio Central"));
        mockMvc.perform(get("/api/v1/inquilino/contratos/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codcon").value(8));
        mockMvc.perform(get("/api/v1/inquilino/contratos/8/cuotas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].montoPendienteRevision").value(500.00));
        mockMvc.perform(get("/api/v1/inquilino/cuotas/22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(1500.00))
                .andExpect(jsonPath("$.situacionVencimiento").value("VENCIDA"));
    }

    private InquilinoContratoResponse contractResponse() {
        return new InquilinoContratoResponse(8, "VIGENTE", LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 9, 1), null, null, new BigDecimal("2500.00"), "BOB",
                new BigDecimal("2500.00"), 4, "Edificio Central", 11, "Departamento 2A");
    }

    private InquilinoCuotaResponse quotaResponse() {
        return new InquilinoCuotaResponse(22, 8, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
                new BigDecimal("2500.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"),
                new BigDecimal("1500.00"), "PARCIAL", "VENCIDA");
    }
}
