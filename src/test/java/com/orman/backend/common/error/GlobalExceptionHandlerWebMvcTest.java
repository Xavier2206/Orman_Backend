package com.orman.backend.common.error;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ErrorTestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsProblemDetailForMissingResource() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/test-errors/not-found"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsProblemDetailForConflict() throws Exception {
        mockMvc.perform(get("/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void returnsProblemDetailForBusinessRuleViolation() throws Exception {
        mockMvc.perform(get("/test-errors/business-rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void returnsSafeProblemDetailForAccessDenied() throws Exception {
        mockMvc.perform(get("/test-errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("No tiene autorización para realizar esta operación."))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.instance").value("/test-errors/access-denied"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ROLE_INTERNO_NO_EXPONIBLE"))));
    }

    @Test
    void returnsOrderedFieldErrorsForValidationFailures() throws Exception {
        mockMvc.perform(post("/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cantidad"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("La cantidad debe ser mayor que cero."))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("nombre"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("El nombre es obligatorio."));
    }

    @Test
    void returnsProblemDetailForInvalidJson() throws Exception {
        mockMvc.perform(post("/test-errors/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no es válido."));
    }

    @Test
    void doesNotExposeInternalDetailsForUnexpectedErrors() throws Exception {
        mockMvc.perform(get("/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno."))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalle-interno-no-exponible"))));
    }
}

@RestController
@RequestMapping("/test-errors")
class ErrorTestController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException("El recurso solicitado no existe.");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new ConflictException("El recurso ya existe.");
    }

    @GetMapping("/business-rule")
    void businessRule() {
        throw new BusinessRuleException("La operación no cumple una regla de negocio.");
    }

    @GetMapping("/access-denied")
    void accessDenied() {
        throw new AccessDeniedException("ROLE_INTERNO_NO_EXPONIBLE");
    }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody ValidationRequest request) {
    }

    @PostMapping("/json")
    void json(@RequestBody JsonRequest request) {
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("detalle-interno-no-exponible");
    }

    private record ValidationRequest(
            @NotBlank(message = "El nombre es obligatorio.") String nombre,
            @Min(value = 1, message = "La cantidad debe ser mayor que cero.") int cantidad) {
    }

    private record JsonRequest(String value) {
    }
}
