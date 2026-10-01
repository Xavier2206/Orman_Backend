package com.orman.backend.payment.controller;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.error.GlobalExceptionHandler;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.service.PagoService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockPart;
import java.nio.charset.StandardCharsets;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagoController.class)
@Import(GlobalExceptionHandler.class)
class PagoControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PagoService pagoService;

    @Test
    void createsPaymentWithCanonicalLocation() throws Exception {
        when(pagoService.create(any(), any(), nullable(org.springframework.web.multipart.MultipartFile.class), any()))
                .thenReturn(response(12));

        mockMvc.perform(multipart("/api/v1/cuotas/8/pagos").part(jsonPart(validJson())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/pagos/12"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.origenRegistro").value("PROPIETARIA"));

        ArgumentCaptor<PagoRequest> requestCaptor = ArgumentCaptor.forClass(PagoRequest.class);
        verify(pagoService).create(eq(8), requestCaptor.capture(), isNull(), any());
        assertThat(requestCaptor.getValue().fechaPago()).isNull();
    }

    @Test
    void returnsProblemDetailForConflictValidationAndInvalidState() throws Exception {
        when(pagoService.create(any(), any(), nullable(org.springframework.web.multipart.MultipartFile.class), any()))
                .thenThrow(new ConflictException("Conflicto de prueba."));
        mockMvc.perform(multipart("/api/v1/cuotas/8/pagos").part(jsonPart(validJson())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        mockMvc.perform(multipart("/api/v1/cuotas/8/pagos").part(jsonPart(
                        validJson().replace("\"monto\":350.00", "\"monto\":0"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/pagos").param("estado", "PAGADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void mapsConditionalPaymentDateRuleToProblemDetail() throws Exception {
        when(pagoService.create(any(), any(), nullable(org.springframework.web.multipart.MultipartFile.class), any()))
                .thenThrow(new BusinessRuleException("Los pagos QR requieren fechaPago."));

        mockMvc.perform(multipart("/api/v1/cuotas/8/pagos").part(jsonPart(qrWithoutPaymentDateJson())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void rejectsTransferenciaForNewPaymentPayloads() throws Exception {
        mockMvc.perform(multipart("/api/v1/cuotas/8/pagos").part(jsonPart(
                        validJson().replace("EFECTIVO", "TRANSFERENCIA"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void listsOnlyThroughThePaginatedPaymentContract() throws Exception {
        when(pagoService.list(any(), any(), any(), any())).thenReturn(
                new PageResponse<>(List.of(response(12)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/pagos").param("estado", "PENDIENTE_REVISION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codpag").value(12));
    }

    @Test
    void annulsPaymentAndReturnsUpdatedPaymentResponse() throws Exception {
        OffsetDateTime revision = OffsetDateTime.of(2026, 9, 25, 14, 0, 0, 0, ZoneOffset.ofHours(-4));
        PagoResponse annulled = new PagoResponse(12, 8, null, new BigDecimal("350.00"), "EFECTIVO",
                OffsetDateTime.of(2026, 9, 25, 17, 0, 0, 0, ZoneOffset.ofHours(-4)), revision,
                "ANULADO", "PROPIETARIA", "carmen", "carmen",
                revision, null, "Se registró por error.");
        when(pagoService.annul(eq(12), any(PagoMotivoRequest.class), any())).thenReturn(annulled);

        mockMvc.perform(patch("/api/v1/pagos/12/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Se registró por error.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codpag").value(12))
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.motivoAnulacion").value("Se registró por error."))
                .andExpect(jsonPath("$.revisadoPor").value("carmen"))
                .andExpect(jsonPath("$.fechaRevision").value("2026-09-25T14:00:00-04:00"));

        verify(pagoService).annul(eq(12), eq(new PagoMotivoRequest("Se registró por error.")), any());
    }

    @Test
    void validatesRequiredAndMaximumLengthForAnnulReason() throws Exception {
        mockMvc.perform(patch("/api/v1/pagos/12/anular")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        String tooLong = "x".repeat(501);
        mockMvc.perform(patch("/api/v1/pagos/12/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void mapsAnnulBusinessRuleToProblemDetail() throws Exception {
        when(pagoService.annul(eq(12), any(PagoMotivoRequest.class), any()))
                .thenThrow(new BusinessRuleException(
                        "Solo pueden anularse pagos confirmados registrados por la propietaria."));

        mockMvc.perform(patch("/api/v1/pagos/12/anular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Corrección\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    private String validJson() {
        return "{\"monto\":350.00,\"metodo\":\"EFECTIVO\","
                + "\"idempotencyKey\":\"11111111-1111-1111-1111-111111111111\"}";
    }

    private String qrWithoutPaymentDateJson() {
        return "{\"monto\":350.00,\"metodo\":\"QR\","
                + "\"idempotencyKey\":\"11111111-1111-1111-1111-111111111111\"}";
    }

    private MockPart jsonPart(String json) {
        MockPart part = new MockPart("pago", json.getBytes(StandardCharsets.UTF_8));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return part;
    }

    private PagoResponse response(Integer codpag) {
        return new PagoResponse(codpag, 8, null, new BigDecimal("350.00"), "EFECTIVO",
                OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, ZoneOffset.ofHours(-4)),
                OffsetDateTime.of(2026, 9, 10, 6, 1, 0, 0, ZoneOffset.ofHours(-4)),
                "CONFIRMADO", "PROPIETARIA", "carmen", "carmen",
                OffsetDateTime.of(2026, 9, 10, 6, 1, 0, 0, ZoneOffset.ofHours(-4)), null, null);
    }
}
