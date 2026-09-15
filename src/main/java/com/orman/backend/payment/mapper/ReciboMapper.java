package com.orman.backend.payment.mapper;

import com.orman.backend.payment.dto.response.ReciboResponse;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.ReciboEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ReciboMapper {

    public ReciboEntity toEntity(PagoEntity pago, LocalDateTime fechaEmision) {
        ReciboEntity recibo = new ReciboEntity();
        recibo.setPago(pago);
        recibo.setFechaEmision(fechaEmision);
        return recibo;
    }

    public ReciboResponse toResponse(ReciboEntity recibo) {
        PagoEntity pago = recibo.getPago();
        return new ReciboResponse(recibo.getCodrec(), pago.getCodpag(), pago.getCuota().getCodcuo(),
                pago.getCuota().getPeriodo(), pago.getMonto(), pago.getCuota().getContrato().getMoneda(),
                pago.getMetodo().name(), pago.getFechaPago(), recibo.getFechaEmision());
    }
}
