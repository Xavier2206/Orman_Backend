package com.orman.backend.property.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.service.UnidadFotoService;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnidadFotoController.class)
@Import(GlobalExceptionHandler.class)
class UnidadFotoControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UnidadFotoService unidadFotoService;

    @Test
    void keepsLegacyJsonAndAcceptsInternalMultipartCreation() throws Exception {
        when(unidadFotoService.create(eq(7), any(), any())).thenReturn(external(10));
        when(unidadFotoService.createInternal(eq(7), any(), any(), any())).thenReturn(internal(11));

        mockMvc.perform(post("/api/v1/unidades/7/fotos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.test/legacy.jpg\",\"titulo\":\"Sala\",\"ambiente\":\"Sala\",\"orden\":0}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/unidades/7/fotos/10"))
                .andExpect(jsonPath("$.url").value("https://example.test/legacy.jpg"))
                .andExpect(jsonPath("$.tieneArchivo").value(false));

        MockMultipartFile foto = new MockMultipartFile("foto", "sala.png", "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/v1/unidades/7/fotos").file(foto)
                        .param("titulo", "Baño principal").param("ambiente", "Baño").param("orden", "1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/unidades/7/fotos/11"))
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.tieneArchivo").value(true));
    }

    @Test
    void exposesListingMetadataReplacementBinaryAndDeletionContracts() throws Exception {
        when(unidadFotoService.listByUnidad(eq(7), any())).thenReturn(List.of(external(10), internal(11)));
        when(unidadFotoService.updateMetadata(eq(7), eq(11), any(), any())).thenReturn(internal(11));
        when(unidadFotoService.replaceArchivo(eq(7), eq(11), any(), any())).thenReturn(internal(11));
        when(unidadFotoService.getArchivo(eq(7), eq(11), any())).thenReturn(
                new UnidadFotoService.UnidadFotoResource(new InputStreamResource(new ByteArrayInputStream(new byte[] {1, 2})),
                        MediaType.IMAGE_JPEG));
        doNothing().when(unidadFotoService).delete(eq(7), eq(11), any());

        mockMvc.perform(get("/api/v1/unidades/7/fotos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tieneArchivo").value(false))
                .andExpect(jsonPath("$[1].url").doesNotExist())
                .andExpect(jsonPath("$[1].tieneArchivo").value(true));
        mockMvc.perform(patch("/api/v1/unidades/7/fotos/11/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Baño principal\",\"ambiente\":\"Baño\",\"orden\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tieneArchivo").value(true));
        mockMvc.perform(multipart("/api/v1/unidades/7/fotos/11/archivo")
                        .file(new MockMultipartFile("foto", "nuevo.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tieneArchivo").value(true));
        mockMvc.perform(get("/api/v1/unidades/7/fotos/11/archivo"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        mockMvc.perform(delete("/api/v1/unidades/7/fotos/11")).andExpect(status().isNoContent());
        verify(unidadFotoService).delete(eq(7), eq(11), isNull());
    }

    @Test
    void reportsMissingMultipartPhotoAsProblemDetail() throws Exception {
        mockMvc.perform(multipart("/api/v1/unidades/7/fotos").param("orden", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    private UnidadFotoResponse external(int id) {
        return new UnidadFotoResponse(id, 7, "https://example.test/legacy.jpg", "Sala", "Sala", 0, false, false);
    }

    private UnidadFotoResponse internal(int id) {
        return new UnidadFotoResponse(id, 7, null, "Baño principal", "Baño", 1, false, true);
    }
}
