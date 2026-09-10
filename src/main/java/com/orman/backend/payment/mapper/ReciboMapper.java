package com.orman.backend.payment.mapper;

import com.orman.backend.payment.dto.response.ReciboResponse;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.ReciboEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ReciboMapper {

    public ReciboEntity toEntity(PagoEntity pago) {
        ReciboEntity recibo = new ReciboEntity();
        recibo.setPago(pago);
        recibo.setFechaEmision(LocalDateTime.now());
        return recibo;
    }

    public ReciboResponse toResponse(ReciboEntity recibo) {
        return new ReciboResponse(recibo.getCodrec(), recibo.getPago().getCodpag(), recibo.getFechaEmision());
    }
}
