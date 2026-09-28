package com.orman.backend.contract.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.contract.dto.request.CuotaListCriteria;
import com.orman.backend.contract.dto.response.CuotaListItemResponse;
import com.orman.backend.contract.service.CuotaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuotaListController.class)
@Import(GlobalExceptionHandler.class)
class CuotaListControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CuotaService cuotaService;

    @Test
    void exposesGlobalPagedListAndKeepsTheSelectedContract() throws Exception {
        CuotaListItemResponse item = new CuotaListItemResponse(1, 25, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1), 10, "Carlos Mendoza", "1234567", 2, "Edificio Norte", 4,
                "Unidad 4", new BigDecimal("2500.00"), new BigDecimal("500.00"), new BigDecimal("200.00"),
                new BigDecimal("2000.00"), "PARCIAL", "VENCIDA");
        when(cuotaService.listGlobal(any(), any())).thenReturn(new PageResponse<>(List.of(item), 0, 20, 1,
                1, true, true));

        mockMvc.perform(get("/api/v1/cuotas")
                        .param("codperInquilino", "10")
                        .param("codcuo", "1894")
                        .param("periodo", "2026-09-01")
                        .param("estado", "PARCIAL")
                        .param("vencimiento", "VENCIDAS")
                        .param("codprop", "2")
                        .param("coduni", "4")
                        .param("conPagoPendienteRevision", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codcuo").value(1))
                .andExpect(jsonPath("$.content[0].nombreCompleto").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.content[0].montoConfirmado").value(500.00))
                .andExpect(jsonPath("$.content[0].montoPendienteRevision").value(200.00))
                .andExpect(jsonPath("$.content[0].saldo").value(2000.00))
                .andExpect(jsonPath("$.content[0].situacionVencimiento").value("VENCIDA"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(cuotaService).listGlobal(org.mockito.ArgumentMatchers.argThat(criteria ->
                criteria.codperInquilino() == 10 && criteria.codcuo() == 1894
                        && criteria.periodo().equals(LocalDate.of(2026, 9, 1))
                        && criteria.estado().name().equals("PARCIAL")
                        && criteria.vencimiento() == CuotaListCriteria.Vencimiento.VENCIDAS
                        && criteria.codprop() == 2 && criteria.coduni() == 4
                        && criteria.conPagoPendienteRevision() && criteria.page() == 0 && criteria.size() == 20),
                any());
    }

    @Test
    void rejectsInvalidFiltersWithProblemDetailBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/cuotas").param("periodo", "2026-09-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("periodo"));

        mockMvc.perform(get("/api/v1/cuotas").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/cuotas").param("codcuo", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("codcuo"));

        verifyNoInteractions(cuotaService);
    }
}
