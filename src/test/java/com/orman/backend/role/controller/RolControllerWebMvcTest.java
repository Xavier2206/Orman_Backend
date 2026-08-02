package com.orman.backend.role.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
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
    void createsRolWithLocation() throws Exception {
        when(rolService.create(any())).thenReturn(response(1, "ADMINISTRADOR", (short) 1));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/roles/1"))
                .andExpect(jsonPath("$.codr").value(1))
                .andExpect(jsonPath("$.nombre").value("ADMINISTRADOR"));
    }

    @Test
    void getsListsUpdatesAndChangesEstado() throws Exception {
        when(rolService.get(1)).thenReturn(response(1, "ADMINISTRADOR", (short) 1));
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ADMINISTRADOR"));

        when(rolService.list(any())).thenReturn(new PageResponse<>(List.of(response(1, "ADMINISTRADOR", (short) 1)),
                0, 20, 1, 1, true, true));
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codr").value(1));

        when(rolService.update(any(), any())).thenReturn(response(1, "SUPERVISOR", (short) 1));
        mockMvc.perform(put(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"SUPERVISOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("SUPERVISOR"));

        when(rolService.deactivate(1)).thenReturn(response(1, "SUPERVISOR", (short) 0));
        when(rolService.activate(1)).thenReturn(response(1, "SUPERVISOR", (short) 1));
        mockMvc.perform(patch(BASE_URL + "/1/desactivar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(0));
        mockMvc.perform(patch(BASE_URL + "/1/activar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(1));
    }

    @Test
    void returnsProblemDetailsForConflictMissingAndInvalidRequests() throws Exception {
        when(rolService.create(any())).thenThrow(new ConflictException("El nombre del Rol ya está registrado."));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));

        when(rolService.get(99)).thenThrow(new ResourceNotFoundException("Rol no encontrado."));
        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
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
