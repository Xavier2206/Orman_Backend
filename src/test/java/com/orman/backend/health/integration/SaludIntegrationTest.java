package com.orman.backend.health.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaludIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void respondePublicamenteCuandoPostgresEjecutaLaConsultaDeSalud() throws Exception {
        mockMvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.baseDatos").value("UP"));
    }
}
