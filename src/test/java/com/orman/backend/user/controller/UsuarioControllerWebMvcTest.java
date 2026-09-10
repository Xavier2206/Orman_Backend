package com.orman.backend.user.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.authorization.service.AuthorizationService;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.user.dto.UsuarioResponse;
import com.orman.backend.user.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(GlobalExceptionHandler.class)
class UsuarioControllerWebMvcTest {

    private static final String BASE_URL = "/api/v1/usuarios";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    void createsUsuarioWithLocationAndWithoutSensitiveFields() throws Exception {
        when(usuarioService.create(any())).thenReturn(response("usuario.demo", (short) 1));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/usuarios/usuario.demo"))
                .andExpect(jsonPath("$.login").value("usuario.demo"))
                .andExpect(jsonPath("$.passwd").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist());
    }

    @Test
    void returnsExpectedProblemDetailsOnCreate() throws Exception {
        when(usuarioService.create(any())).thenThrow(new ConflictException("El login ya está registrado."));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));

        reset(usuarioService);
        when(usuarioService.create(any())).thenThrow(new ResourceNotFoundException("Persona no encontrada."));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(validCreateJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getsAndListsUsuarios() throws Exception {
        when(usuarioService.get("usuario.demo")).thenReturn(response("usuario.demo", (short) 1));
        mockMvc.perform(get(BASE_URL + "/usuario.demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("usuario.demo"))
                .andExpect(jsonPath("$.passwd").doesNotExist());

        when(usuarioService.list(any())).thenReturn(new PageResponse<>(List.of(response("usuario.demo", (short) 1)),
                0, 20, 1, 1, true, true));
        when(authorizationService.isOwner(any())).thenReturn(true);
        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].login").value("usuario.demo"))
                .andExpect(jsonPath("$.content[0].passwd").doesNotExist());
    }

    @Test
    void forwardsRemoteQueryAndKeepsPagination() throws Exception {
        when(usuarioService.list(eq("Xavier"), any())).thenReturn(new PageResponse<>(
                List.of(new UsuarioResponse("xavier.login", (short) 1, 7,
                        LocalDateTime.of(2026, 1, 1, 0, 0), null, "Xavier", "Ortega", null)),
                0, 5, 1, 1, true, true));
        when(authorizationService.isOwner(any())).thenReturn(true);

        mockMvc.perform(get(BASE_URL).param("q", "Xavier").param("page", "0").param("size", "5")
                        .param("sort", "login,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].login").value("xavier.login"))
                .andExpect(jsonPath("$.content[0].nombre").value("Xavier"))
                .andExpect(jsonPath("$.content[0].ap").value("Ortega"))
                .andExpect(jsonPath("$.content[0].am").doesNotExist());
    }

    @Test
    void handlesMissingUsuarioAndUpdatesEstado() throws Exception {
        when(usuarioService.get("inexistente")).thenThrow(new ResourceNotFoundException("Usuario no encontrado."));
        mockMvc.perform(get(BASE_URL + "/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        when(usuarioService.update(any(), any())).thenReturn(response("usuario.demo", (short) 0));
        mockMvc.perform(put(BASE_URL + "/usuario.demo").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(0));
    }

    @Test
    void activatesAndDeactivates() throws Exception {
        when(usuarioService.deactivate("usuario.demo")).thenReturn(response("usuario.demo", (short) 0));
        when(usuarioService.activate("usuario.demo")).thenReturn(response("usuario.demo", (short) 1));
        mockMvc.perform(patch(BASE_URL + "/usuario.demo/desactivar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(0));
        mockMvc.perform(patch(BASE_URL + "/usuario.demo/activar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(1));
    }

    @Test
    void changesPasswordWithoutResponseBody() throws Exception {
        mockMvc.perform(put(BASE_URL + "/usuario.demo/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"clave-ficticia\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        doThrow(new ResourceNotFoundException("Usuario no encontrado.")).when(usuarioService)
                .changePassword(any(), any());
        mockMvc.perform(put(BASE_URL + "/inexistente/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"clave-ficticia\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidAndMalformedRequests() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"\",\"password\":\"corta\",\"codper\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        mockMvc.perform(put(BASE_URL + "/usuario.demo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private String validCreateJson() {
        return "{\"login\":\"usuario.demo\",\"password\":\"clave-ficticia\",\"codper\":7}";
    }

    private UsuarioResponse response(String login, short estado) {
        return new UsuarioResponse(login, estado, 7, LocalDateTime.of(2026, 1, 1, 0, 0), null);
    }
}
