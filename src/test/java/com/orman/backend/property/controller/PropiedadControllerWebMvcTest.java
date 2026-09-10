package com.orman.backend.property.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.service.PropiedadService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PropiedadController.class)
@Import(GlobalExceptionHandler.class)
class PropiedadControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/propiedades";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PropiedadService propiedadService;

    @Test
    void createsAPropertyWithLocationAndResponseBody() throws Exception {
        when(propiedadService.create(any(), any())).thenReturn(response(12));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/propiedades/12"))
                .andExpect(jsonPath("$.codprop").value(12))
                .andExpect(jsonPath("$.codperPropietaria").value(7));
    }

    @Test
    void returnsProblemDetailForConflictsAndInvalidRequests() throws Exception {
        when(propiedadService.create(any(), any())).thenThrow(new ConflictException("Conflicto de prueba."));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("\"inversionInicial\":1000.00", "\"inversionInicial\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'inversionInicial')]").isNotEmpty());
    }

    @Test
    void listsOnlyThroughThePaginatedContractAndRejectsInvalidFilters() throws Exception {
        when(propiedadService.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));
        mockMvc.perform(get(BASE_URL).param("tipo", "CASA").param("estado", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codprop").value(12));

        mockMvc.perform(get(BASE_URL).param("tipo", "LOCAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private String validJson() {
        return "{\"nombre\":\"Casa Sur\",\"tipo\":\"CASA\",\"direccion\":\"Calle 1\","
                + "\"ciudad\":\"La Paz\",\"codperPropietaria\":7,\"inversionInicial\":1000.00,\"estado\":1}";
    }

    private PropiedadResponse response(Integer codprop) {
        return new PropiedadResponse(codprop, "Casa Sur", "CASA", "Calle 1", "La Paz", null, null, null,
                null, 7, new BigDecimal("1000.00"), (short) 1);
    }
}
