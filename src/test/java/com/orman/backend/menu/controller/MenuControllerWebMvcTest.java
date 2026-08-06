package com.orman.backend.menu.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
}
