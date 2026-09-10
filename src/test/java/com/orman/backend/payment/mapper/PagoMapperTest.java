package com.orman.backend.payment.mapper;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.person.entity.Persona;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PagoMapperTest {

    private final PagoMapper pagoMapper = new PagoMapper();
    private final PagoComprobanteMapper comprobanteMapper = new PagoComprobanteMapper();
    private final CuentaPagoMapper cuentaPagoMapper = new CuentaPagoMapper();

    @Test
    void mapsPendingPaymentProofAndAccountWithoutExposingEntities() {
        CuotaEntity cuota = new CuotaEntity();
        ReflectionTestUtils.setField(cuota, "codcuo", 18);
        CuentaPagoEntity cuenta = new CuentaPagoEntity();
        ReflectionTestUtils.setField(cuenta, "codcta", 7);
        var pago = pagoMapper.toPendingEntity(new PagoRequest(new BigDecimal("350.00"), MetodoPago.TRANSFERENCIA,
                7, " REF-01 ", LocalDateTime.of(2026, 9, 10, 10, 0), UUID.randomUUID()), cuota, cuenta);
        ReflectionTestUtils.setField(pago, "codpag", 11);

        assertThat(pago.getEstado()).isEqualTo(PagoEstado.PENDIENTE_REVISION);
        assertThat(pago.getReferenciaExterna()).isEqualTo("REF-01");
        assertThat(pagoMapper.toResponse(pago).codcta()).isEqualTo(7);

        PagoComprobanteEntity comprobante = comprobanteMapper.toEntity(new PagoComprobanteRequest(
                "https://example.test/comprobante.pdf", " comprobante.pdf ", "application/pdf", 0), pago);
        ReflectionTestUtils.setField(comprobante, "id", 3);
        assertThat(comprobanteMapper.toResponse(comprobante).nombreArchivo()).isEqualTo("comprobante.pdf");

        Persona propietaria = new Persona();
        ReflectionTestUtils.setField(propietaria, "codper", 5);
        CuentaPagoEntity cuentaMapeada = cuentaPagoMapper.toEntity(new CuentaPagoRequest(" Banco ", " 123 ",
                " Titular ", "", " ", 0, "1"), propietaria);
        ReflectionTestUtils.setField(cuentaMapeada, "codcta", 7);
        assertThat(cuentaPagoMapper.toResponse(cuentaMapeada).qrUrl()).isNull();
        assertThat(cuentaPagoMapper.toResponse(cuentaMapeada).codperPropietaria()).isEqualTo(5);
    }
}
