package com.orman.backend.payment.mapper;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.entity.QrCobroEntity;
import com.orman.backend.user.entity.Usuario;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoEntity toEntity(PagoRequest request, CuotaEntity cuota, QrCobroEntity qrCobro,
                               OrigenRegistroPago origenRegistro, Usuario registradoPor,
                               LocalDateTime fechaPagoResuelta, LocalDateTime fechaRegistro) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setQrCobro(qrCobro);
        pago.setMonto(request.monto());
        pago.setMetodo(request.metodo());
        pago.setFechaPago(fechaPagoResuelta);
        pago.setFechaRegistro(fechaRegistro);
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(origenRegistro);
        pago.setRegistradoPor(registradoPor);
        pago.setIdempotencyKey(request.idempotencyKey());
        return pago;
    }

    public PagoResponse toResponse(PagoEntity pago) {
        Integer codqr = pago.getQrCobro() == null ? null : pago.getQrCobro().getCodqr();
        return new PagoResponse(pago.getCodpag(), pago.getCuota().getCodcuo(), codqr, pago.getMonto(),
                pago.getMetodo().name(), OrmanTimeConfig.ormanLocalToOffset(pago.getFechaPago()),
                OrmanTimeConfig.ormanLocalToOffset(pago.getFechaRegistro()),
                pago.getEstado().name(), pago.getOrigenRegistro().name(), pago.getRegistradoPor().getLogin(),
                pago.getRevisadoPor() == null ? null : pago.getRevisadoPor().getLogin(),
                OrmanTimeConfig.ormanLocalToOffset(pago.getFechaRevision()),
                pago.getMotivoRechazo(), pago.getMotivoAnulacion());
    }
}
