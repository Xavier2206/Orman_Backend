package com.orman.backend.contract.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.service.ContratoArchivoContent;
import com.orman.backend.contract.service.ContratoArchivoService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContratoArchivoController.class)
@Import(GlobalExceptionHandler.class)
class ContratoArchivoControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContratoArchivoService contratoArchivoService;

    @Test
    void acceptsMultipartPdfAndReturnsMetadataWithoutPublicUrlOrPath() throws Exception {
        when(contratoArchivoService.create(eq(42), any(), eq(0), any())).thenReturn(response());
        MockMultipartFile pdf = new MockMultipartFile("archivo", "contrato firmado.pdf", "application/pdf",
                new byte[] {'%', 'P', 'D', 'F', '-'});

        mockMvc.perform(multipart("/api/v1/contratos/42/archivos").file(pdf).param("orden", "0"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/v1/contratos/42/archivos/7/download"))
                .andExpect(jsonPath("$.codarc").value(7))
                .andExpect(jsonPath("$.nombreArchivo").value("contrato firmado.pdf"))
                .andExpect(jsonPath("$.tipoContenido").value("application/pdf"))
                .andExpect(jsonPath("$.tamanoFinal").value(1234))
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.rutaRef").doesNotExist());

        verify(contratoArchivoService).create(eq(42), any(), eq(0), any());
    }

    @Test
    void listsMetadataDownloadsWithPrivateHeadersAndDeletes() throws Exception {
        when(contratoArchivoService.listByContrato(eq(42), any())).thenReturn(List.of(response()));
        byte[] contentBytes = new byte[] {'%', 'P', 'D', 'F', '-'};
        when(contratoArchivoService.download(eq(42), eq(7), any())).thenReturn(
                new ContratoArchivoContent(new ByteArrayResource(contentBytes), "contrato.pdf", contentBytes.length));
        doNothing().when(contratoArchivoService).delete(eq(42), eq(7), any());

        mockMvc.perform(get("/api/v1/contratos/42/archivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codarc").value(7))
                .andExpect(jsonPath("$[0].almacenadoInternamente").value(true))
                .andExpect(jsonPath("$[0].url").doesNotExist());

        mockMvc.perform(get("/api/v1/contratos/42/archivos/7/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(contentBytes))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(delete("/api/v1/contratos/42/archivos/7"))
                .andExpect(status().isNoContent());
        verify(contratoArchivoService).delete(eq(42), eq(7), any());
    }

    @Test
    void rejectsMissingFileAndNegativeOrder() throws Exception {
        mockMvc.perform(multipart("/api/v1/contratos/42/archivos").param("orden", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(multipart("/api/v1/contratos/42/archivos")
                        .file(new MockMultipartFile("archivo", "contrato.pdf", "application/pdf", new byte[] {1}))
                        .param("orden", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private ContratoArchivoResponse response() {
        return new ContratoArchivoResponse(7, 42, "contrato firmado.pdf", "application/pdf", 1450L,
                1234L, LocalDateTime.of(2026, 9, 16, 12, 0), "owner.test", 0, true);
    }
}
