package com.orman.backend.payment.mapper;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.entity.QrCobroEntity;
import com.orman.backend.payment.service.StoredPaymentImage;
import com.orman.backend.user.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PagoMapperTest {

    private final PagoMapper pagoMapper = new PagoMapper();
    private final PagoComprobanteMapper comprobanteMapper = new PagoComprobanteMapper();

    @Test
    void mapsQrPaymentAndPrivateProofMetadataWithoutExposingStoragePath() {
        CuotaEntity cuota = new CuotaEntity();
        ReflectionTestUtils.setField(cuota, "codcuo", 18);
        QrCobroEntity qr = new QrCobroEntity();
        ReflectionTestUtils.setField(qr, "codqr", 7);
        Usuario actor = new Usuario();
        actor.setLogin("tenant.mapper");
        LocalDateTime fechaPago = LocalDateTime.of(2026, 9, 10, 10, 0);
        LocalDateTime fechaRegistro = LocalDateTime.of(2026, 9, 10, 10, 1);
        var pago = pagoMapper.toEntity(new PagoRequest(new BigDecimal("350.00"), MetodoPago.QR, fechaPago,
                UUID.randomUUID()), cuota, qr, OrigenRegistroPago.INQUILINO, actor, fechaPago, fechaRegistro);
        ReflectionTestUtils.setField(pago, "codpag", 11);

        assertThat(pago.getEstado()).isEqualTo(PagoEstado.PENDIENTE_REVISION);
        assertThat(pago.getFechaPago()).isEqualTo(fechaPago);
        assertThat(pago.getFechaRegistro()).isEqualTo(fechaRegistro);
        assertThat(pago.getOrigenRegistro()).isEqualTo(OrigenRegistroPago.INQUILINO);
        pago.setFechaRevision(LocalDateTime.of(2026, 9, 10, 12, 25));
        pago.setRevisadoPor(actor);
        assertThat(pagoMapper.toResponse(pago).registradoPor()).isEqualTo("tenant.mapper");
        assertThat(pagoMapper.toResponse(pago).codqr()).isEqualTo(7);
        assertThat(pagoMapper.toResponse(pago).fechaPago()).isEqualTo(OffsetDateTime.of(
                2026, 9, 10, 10, 0, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(pagoMapper.toResponse(pago).fechaRegistro()).isEqualTo(OffsetDateTime.of(
                2026, 9, 10, 10, 1, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(pagoMapper.toResponse(pago).fechaRevision()).isEqualTo(OffsetDateTime.of(
                2026, 9, 10, 12, 25, 0, 0, ZoneOffset.ofHours(-4)));

        var image = new StoredPaymentImage("comprobantes/11/uuid.png", "comprobante.png", "image/png");
        PagoComprobanteEntity comprobante = comprobanteMapper.toEntity(image, pago, fechaRegistro);
        ReflectionTestUtils.setField(comprobante, "id", 3);
        var response = comprobanteMapper.toResponse(comprobante);
        assertThat(response.nombreArchivo()).isEqualTo("comprobante.png");
        assertThat(response.tipoContenido()).isEqualTo("image/png");
        assertThat(response.fechaRegistro()).isEqualTo(OffsetDateTime.of(
                2026, 9, 10, 10, 1, 0, 0, ZoneOffset.ofHours(-4)));
        assertThat(response.toString()).doesNotContain("comprobantes/11/uuid.png");
    }
}
