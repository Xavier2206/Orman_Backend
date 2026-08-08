package com.orman.backend.person.controller;

import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.service.PersonaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonaController.class)
@Import(GlobalExceptionHandler.class)
class PersonaControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/personas";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PersonaService personaService;

    @Test
    void createsPersonaWithLocationAndResponseBody() throws Exception {
        when(personaService.create(any())).thenReturn(response(7, (short) 1));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/personas/7"))
                .andExpect(jsonPath("$.codper").value(7))
                .andExpect(jsonPath("$.estado").value(1));
    }

    @Test
    void returnsProblemDetailForDuplicateCiOnCreate() throws Exception {
        when(personaService.create(any())).thenThrow(new ConflictException("El CI ya está registrado."));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(BASE_URL))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void getsPersonaAndReturnsNotFoundWhenMissing() throws Exception {
        when(personaService.get(7)).thenReturn(response(7, (short) 1));
        mockMvc.perform(get(BASE_URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codper").value(7));

        when(personaService.get(8)).thenThrow(new ResourceNotFoundException("Persona no encontrada."));
        mockMvc.perform(get(BASE_URL + "/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value(BASE_URL + "/8"));
    }

    @Test
    void listsPaginatedResponse() throws Exception {
        when(personaService.list(any())).thenReturn(new PageResponse<>(List.of(response(7, (short) 1)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codper").value(7))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void updatesPersonaAndHandlesConflict() throws Exception {
        when(personaService.update(any(), any())).thenReturn(response(7, (short) 1));
        mockMvc.perform(put(BASE_URL + "/7").contentType(MediaType.APPLICATION_JSON).content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre válido"));

        when(personaService.update(any(), any())).thenThrow(new ConflictException("El CI ya está registrado."));
        mockMvc.perform(put(BASE_URL + "/7").contentType(MediaType.APPLICATION_JSON).content(validUpdateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void deletesPhysicallyAndHandlesMissingPersona() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        doThrow(new ResourceNotFoundException("Persona no encontrada.")).when(personaService).delete(8);
        mockMvc.perform(delete(BASE_URL + "/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void deactivatesAndActivatesAndHandlesMissingPersonas() throws Exception {
        when(personaService.deactivate(7)).thenReturn(response(7, (short) 0));
        mockMvc.perform(patch(BASE_URL + "/7/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(0));
        when(personaService.deactivate(8)).thenThrow(new ResourceNotFoundException("Persona no encontrada."));
        mockMvc.perform(patch(BASE_URL + "/8/desactivar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        when(personaService.activate(7)).thenReturn(response(7, (short) 1));
        mockMvc.perform(patch(BASE_URL + "/7/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(1));
        when(personaService.activate(8)).thenThrow(new ResourceNotFoundException("Persona no encontrada."));
        mockMvc.perform(patch(BASE_URL + "/8/activar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidCreateRequests(String body, String field) throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == '" + field + "')]").isNotEmpty());
    }

    @Test
    void rejectsInvalidUpdateAndMalformedJson() throws Exception {
        mockMvc.perform(put(BASE_URL + "/7").contentType(MediaType.APPLICATION_JSON).content("{\"ci\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(BASE_URL));
    }

    @Test
    void rejectsMissingNullEmptyAndBlankEmailOnCreateAndCompleteUpdate() throws Exception {
        String omitted = validCreateJson().replace(",\"correo\":\"persona@example.test\"", "");
        String nullEmail = validCreateJson().replace("\"persona@example.test\"", "null");

        for (String body : List.of(omitted, nullEmail, json("correo", ""), json("correo", "   "))) {
            mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
            mockMvc.perform(put(BASE_URL + "/7").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void acceptsValidEmailOfExactlyOneHundredCharacters() throws Exception {
        String email = "a".repeat(64) + "@" + "b".repeat(31) + ".com";
        when(personaService.create(any())).thenReturn(response(7, (short) 1));
        when(personaService.update(any(), any())).thenReturn(response(7, (short) 1));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(json("correo", email)))
                .andExpect(status().isCreated());
        mockMvc.perform(put(BASE_URL + "/7").contentType(MediaType.APPLICATION_JSON).content(json("correo", email)))
                .andExpect(status().isOk());
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(json("ci", ""), "ci"),
                Arguments.of(json("ci", "123456789012345678901"), "ci"),
                Arguments.of(json("nombre", ""), "nombre"),
                Arguments.of(json("nombre", "n".repeat(61)), "nombre"),
                Arguments.of(json("ap", "a".repeat(41)), "ap"),
                Arguments.of(json("am", "a".repeat(41)), "am"),
                Arguments.of(json("genero", "X"), "genero"),
                Arguments.of(json("estado", "2"), "estado"),
                Arguments.of(json("correo", "   "), "correo"),
                Arguments.of(json("correo", "correo-invalido"), "correo"),
                Arguments.of(json("correo", "c".repeat(90) + "@example.test"), "correo"),
                Arguments.of(json("telefono", ""), "telefono"),
                Arguments.of(json("telefono", "7".repeat(21)), "telefono"),
                Arguments.of(json("tipoPersona", "X"), "tipoPersona"),
                Arguments.of(json("foto", "f".repeat(256)), "foto"));
    }

    private static String json(String field, String value) {
        return "{\"ci\":\"" + valueFor(field, "ci", value, "CI-001") + "\",\"nombre\":\""
                + valueFor(field, "nombre", value, "Nombre válido") + "\",\"ap\":\""
                + valueFor(field, "ap", value, "Paterno") + "\",\"am\":\""
                + valueFor(field, "am", value, "Materno") + "\",\"genero\":\""
                + valueFor(field, "genero", value, "F") + "\",\"estado\":\""
                + valueFor(field, "estado", value, "1") + "\",\"correo\":\""
                + valueFor(field, "correo", value, "persona@example.test") + "\",\"telefono\":\""
                + valueFor(field, "telefono", value, "70000000") + "\",\"tipoPersona\":\""
                + valueFor(field, "tipoPersona", value, "A") + "\",\"foto\":\""
                + valueFor(field, "foto", value, "foto") + "\"}";
    }

    private static String valueFor(String field, String expectedField, String value, String defaultValue) {
        return field.equals(expectedField) ? value : defaultValue;
    }

    private String validCreateJson() {
        return "{\"ci\":\"CI-001\",\"nombre\":\"Nombre válido\",\"genero\":\"F\",\"estado\":\"1\",\"correo\":\"persona@example.test\",\"telefono\":\"70000000\",\"tipoPersona\":\"A\"}";
    }

    private String validUpdateJson() {
        return validCreateJson();
    }

    private PersonaResponse response(Integer codper, short estado) {
        return new PersonaResponse(codper, "CI-001", "Nombre válido", null, null, 'F', estado, "persona@example.test",
                "70000000", 'A', null, LocalDateTime.of(2026, 1, 1, 0, 0));
    }
}
