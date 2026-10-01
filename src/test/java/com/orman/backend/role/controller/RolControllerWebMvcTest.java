package com.orman.backend.role.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.service.RolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolController.class)
@Import(GlobalExceptionHandler.class)
class RolControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/roles";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RolService rolService;

    @Test
    void exposesOnlyFixedRoleQueries() throws Exception {
        when(rolService.get(1)).thenReturn(response(1, "PROPIETARIO", (short) 1));
        when(rolService.list(any(), any(), any())).thenReturn(new PageResponse<>(
                List.of(response(1, "PROPIETARIO", (short) 1), response(2, "INQUILINO", (short) 1)),
                0, 20, 2, 1, true, true));
        mockMvc.perform(get(BASE_URL + "/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("PROPIETARIO"));
        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"OTRO\"}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(patch(BASE_URL + "/1/activar")).andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE_URL + "/1/desactivar")).andExpect(status().isNotFound());
    }

    @Test
    void listsWithCombinedFiltersAndReturnsGlobalResumen() throws Exception {
        when(rolService.list(eq(" prop "), eq((short) 1), any())).thenReturn(new PageResponse<>(
                List.of(response(1, "PROPIETARIO", (short) 1)), 0, 10, 1, 1, true, true));

        mockMvc.perform(get(BASE_URL).param("q", " prop ").param("estado", "1")
                        .param("page", "0").param("size", "10").param("sort", "nombre,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("PROPIETARIO"))
                .andExpect(jsonPath("$.totalElements").value(1));

        when(rolService.resumen()).thenReturn(new RolResumenResponse(2, 2, 0));
        mockMvc.perform(get(BASE_URL + "/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRoles").value(2))
                .andExpect(jsonPath("$.activos").value(2))
                .andExpect(jsonPath("$.inactivos").value(0));
    }

    @Test
    void returnsProblemDetailForMissingRoleAndInvalidFilter() throws Exception {
        when(rolService.get(99)).thenThrow(new ResourceNotFoundException("Rol no encontrado."));
        mockMvc.perform(get(BASE_URL + "/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(get(BASE_URL).param("estado", "2")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void doesNotExposePhysicalDeleteEndpoint() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    private RolResponse response(Integer codr, String nombre, short estado) {
        return new RolResponse(codr, nombre, estado);
    }
}
