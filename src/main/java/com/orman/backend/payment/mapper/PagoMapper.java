package com.orman.backend.payment.mapper;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoEntity toEntity(PagoRequest request, CuotaEntity cuota, CuentaPagoEntity cuentaPago,
                               OrigenRegistroPago origenRegistro, Usuario registradoPor,
                               LocalDateTime fechaRegistro) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setCuentaPago(cuentaPago);
        pago.setMonto(request.monto());
        pago.setMetodo(request.metodo());
        pago.setReferenciaExterna(trimToNull(request.referenciaExterna()));
        pago.setFechaPago(request.fechaPago());
        pago.setFechaRegistro(fechaRegistro);
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(origenRegistro);
        pago.setRegistradoPor(registradoPor);
        pago.setIdempotencyKey(request.idempotencyKey());
        return pago;
    }

    public PagoResponse toResponse(PagoEntity pago) {
        Integer codcta = pago.getCuentaPago() == null ? null : pago.getCuentaPago().getCodcta();
        return new PagoResponse(pago.getCodpag(), pago.getCuota().getCodcuo(), codcta, pago.getMonto(),
                pago.getMetodo().name(), pago.getReferenciaExterna(), pago.getFechaPago(), pago.getFechaRegistro(),
                pago.getEstado().name(), pago.getOrigenRegistro().name(), pago.getRegistradoPor().getLogin(),
                pago.getRevisadoPor() == null ? null : pago.getRevisadoPor().getLogin(), pago.getFechaRevision(),
                pago.getMotivoRechazo(), pago.getMotivoAnulacion());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
