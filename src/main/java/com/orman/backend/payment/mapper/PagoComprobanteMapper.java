package com.orman.backend.payment.mapper;

import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PagoComprobanteMapper {

    public PagoComprobanteEntity toEntity(PagoComprobanteRequest request, PagoEntity pago) {
        PagoComprobanteEntity comprobante = new PagoComprobanteEntity();
        comprobante.setPago(pago);
        comprobante.setUrl(request.url().trim());
        comprobante.setNombreArchivo(request.nombreArchivo().trim());
        comprobante.setTipoContenido(request.tipoContenido().trim());
        comprobante.setOrden(request.orden());
        comprobante.setFechaRegistro(LocalDateTime.now());
        return comprobante;
    }

    public PagoComprobanteResponse toResponse(PagoComprobanteEntity comprobante) {
        return new PagoComprobanteResponse(comprobante.getId(), comprobante.getPago().getCodpag(), comprobante.getUrl(),
                comprobante.getNombreArchivo(), comprobante.getTipoContenido(), comprobante.getOrden(),
                comprobante.getFechaRegistro());
    }
}
