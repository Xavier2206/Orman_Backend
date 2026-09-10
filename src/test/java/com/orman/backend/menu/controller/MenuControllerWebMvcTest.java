package com.orman.backend.menu.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.menu.dto.response.MenuResumenResponse;
import com.orman.backend.menu.dto.response.MenuResponse;
import com.orman.backend.menu.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
@Import(GlobalExceptionHandler.class)
class MenuControllerWebMvcTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MenuService menuService;
    @Test void createsMenuAndDoesNotExposePhysicalDelete() throws Exception {
        when(menuService.create(any())).thenReturn(new MenuResponse(4, "PERSONAS", "users", (short) 1));
        mockMvc.perform(post("/api/v1/menus").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"PERSONAS\",\"icono\":\"users\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/v1/menus/4"))
                .andExpect(jsonPath("$.estado").value(1));
        mockMvc.perform(delete("/api/v1/menus/4")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void listsWithFiltersReturnsResumenAndRejectsInvalidEstado() throws Exception {
        when(menuService.list(eq(" control "), eq((short) 1), any())).thenReturn(new PageResponse<>(
                java.util.List.of(new MenuResponse(4, "CONTROL DE ACCESO", "shield", (short) 1)),
                0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/menus").param("q", " control ").param("estado", "1")
                        .param("page", "0").param("size", "10").param("sort", "nombre,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("CONTROL DE ACCESO"))
                .andExpect(jsonPath("$.totalElements").value(1));

        when(menuService.resumen()).thenReturn(new MenuResumenResponse(5, 3, 2));
        mockMvc.perform(get("/api/v1/menus/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMenus").value(5))
                .andExpect(jsonPath("$.activos").value(3))
                .andExpect(jsonPath("$.inactivos").value(2));

        mockMvc.perform(get("/api/v1/menus").param("estado", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
