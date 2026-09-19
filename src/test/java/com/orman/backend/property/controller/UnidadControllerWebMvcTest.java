package com.orman.backend.property.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.service.UnidadService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnidadController.class)
@Import(GlobalExceptionHandler.class)
class UnidadControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/propiedades/161/unidades";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UnidadService unidadService;

    @Test
    void acceptsTheOptionalOperationalStateFilterAndPreservesThePageContract() throws Exception {
        when(unidadService.listByPropiedad(eq(161), eq((short) 1), any(), any()))
                .thenReturn(new PageResponse<>(List.of(response(501, (short) 1)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get(BASE_URL).param("estadoOperativo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].coduni").value(501))
                .andExpect(jsonPath("$.content[0].estadoOperativo").value(1))
                .andExpect(jsonPath("$.content[0].disponibleParaContrato").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(unidadService).listByPropiedad(eq(161), eq((short) 1), any(), any());
    }

    @Test
    void keepsTheUnfilteredCallCompatible() throws Exception {
        when(unidadService.listByPropiedad(eq(161), eq((Short) null), any(), any()))
                .thenReturn(new PageResponse<>(List.of(response(501, (short) 1)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(unidadService).listByPropiedad(eq(161), eq((Short) null), any(), any());
    }

    @Test
    void rejectsOperationalStateValuesOutsideZeroAndOne() throws Exception {
        mockMvc.perform(get(BASE_URL).param("estadoOperativo", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("estadoOperativo"));

        verify(unidadService, never()).listByPropiedad(any(), any(), any(), any());
    }

    @Test
    void exposesActivateAndDeactivatePatchActionsWithTheUpdatedResource() throws Exception {
        when(unidadService.activate(eq(501), any())).thenReturn(response(501, (short) 1));
        when(unidadService.deactivate(eq(501), any())).thenReturn(response(501, (short) 0));

        mockMvc.perform(patch("/api/v1/unidades/501/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coduni").value(501))
                .andExpect(jsonPath("$.estadoOperativo").value(1));
        mockMvc.perform(patch("/api/v1/unidades/501/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coduni").value(501))
                .andExpect(jsonPath("$.estadoOperativo").value(0));

        verify(unidadService).activate(eq(501), any());
        verify(unidadService).deactivate(eq(501), any());
    }

    @Test
    void returnsProblemDetailWhenDeactivationIsBlockedByACurrentContract() throws Exception {
        when(unidadService.deactivate(eq(501), any()))
                .thenThrow(new BusinessRuleException("No se puede desactivar la unidad porque tiene un contrato vigente."));

        mockMvc.perform(patch("/api/v1/unidades/501/desactivar"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede desactivar la unidad porque tiene un contrato vigente."));
    }

    @Test
    void keepsPutAvailableWhenTheRequestStateMatchesTheCurrentState() throws Exception {
        when(unidadService.update(eq(501), any(UnidadRequest.class), any()))
                .thenReturn(response(501, (short) 1));

        mockMvc.perform(put("/api/v1/unidades/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUnidadJson((short) 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value(1));

        verify(unidadService).update(eq(501), any(UnidadRequest.class), any());
    }

    @Test
    void returnsProblemDetailWhenPutAttemptsToChangeOperationalState() throws Exception {
        when(unidadService.update(eq(501), any(UnidadRequest.class), any()))
                .thenThrow(new BusinessRuleException(
                        "El estado operativo de la unidad debe modificarse mediante las acciones de activar o desactivar."));

        mockMvc.perform(put("/api/v1/unidades/501")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUnidadJson((short) 0)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.detail")
                        .value("El estado operativo de la unidad debe modificarse mediante las acciones de activar o desactivar."));
    }

    @Test
    void protectsStateActionsWithTheOwnerRoleAnnotation() {
        PreAuthorize authorization = UnidadController.class.getAnnotation(PreAuthorize.class);

        org.assertj.core.api.Assertions.assertThat(authorization).isNotNull();
        org.assertj.core.api.Assertions.assertThat(authorization.value())
                .isEqualTo("hasRole('PROPIETARIO')");
    }

    private String validUnidadJson(short estadoOperativo) {
        return "{\"nombre\":\"Unidad 101\",\"tipoUnidad\":\"DEPARTAMENTO\","
                + "\"descripcion\":null,\"area\":40.00,\"dormitorios\":1,\"banos\":1,"
                + "\"piso\":1,\"ubicacionInterna\":\"Bloque A\",\"precioBase\":2500.00,"
                + "\"estadoOperativo\":" + estadoOperativo + "}";
    }

    private UnidadResponse response(Integer coduni, Short estadoOperativo) {
        return new UnidadResponse(coduni, 161, "Unidad 101", "DEPARTAMENTO", null,
                new BigDecimal("45.50"), (short) 1, (short) 1, 1, "Torre A",
                new BigDecimal("2500.00"), estadoOperativo, true);
    }
}
