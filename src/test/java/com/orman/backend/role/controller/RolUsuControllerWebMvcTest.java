package com.orman.backend.role.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.response.RolUsuResponse;
import com.orman.backend.role.service.RolUsuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolUsuController.class)
@Import(GlobalExceptionHandler.class)
class RolUsuControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RolUsuService rolUsuService;

    @Test
    void assignsWithLocationAndDoesNotExposeSensitiveFields() throws Exception {
        when(rolUsuService.assign("usuario.demo", 1)).thenReturn(response());

        mockMvc.perform(post("/api/v1/usuarios/usuario.demo/roles/1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/usuarios/usuario.demo/roles/1"))
                .andExpect(jsonPath("$.login").value("usuario.demo"))
                .andExpect(jsonPath("$.nombreRol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.passwd").doesNotExist())
                .andExpect(jsonPath("$.persona").doesNotExist());
    }

    @Test
    void removesAndListsAssignments() throws Exception {
        mockMvc.perform(delete("/api/v1/usuarios/usuario.demo/roles/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        when(rolUsuService.listByUsuario("usuario.demo")).thenReturn(List.of(response()));
        mockMvc.perform(get("/api/v1/usuarios/usuario.demo/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codr").value(1));

        when(rolUsuService.listByRol(1)).thenReturn(List.of(response()));
        mockMvc.perform(get("/api/v1/roles/1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("usuario.demo"));
    }

    @Test
    void returnsExpectedProblemDetails() throws Exception {
        when(rolUsuService.assign("usuario.demo", 1)).thenThrow(new ConflictException("El Rol ya está asignado al Usuario."));
        mockMvc.perform(post("/api/v1/usuarios/usuario.demo/roles/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));

        when(rolUsuService.assign("usuario.demo", 2)).thenThrow(new BusinessRuleException("No se puede asignar un Rol inactivo."));
        mockMvc.perform(post("/api/v1/usuarios/usuario.demo/roles/2"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        doThrow(new ResourceNotFoundException("La asignación de Rol no existe.")).when(rolUsuService)
                .remove("usuario.demo", 1);
        mockMvc.perform(delete("/api/v1/usuarios/usuario.demo/roles/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    private RolUsuResponse response() {
        return new RolUsuResponse("usuario.demo", 1, "ADMINISTRADOR", LocalDateTime.of(2026, 8, 2, 12, 0));
    }
}
