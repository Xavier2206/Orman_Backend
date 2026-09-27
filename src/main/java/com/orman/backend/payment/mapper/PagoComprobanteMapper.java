package com.orman.backend.payment.mapper;

import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.service.StoredPaymentImage;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PagoComprobanteMapper {

    public PagoComprobanteEntity toEntity(StoredPaymentImage image, PagoEntity pago, LocalDateTime fechaRegistro) {
        PagoComprobanteEntity comprobante = new PagoComprobanteEntity();
        comprobante.setPago(pago);
        comprobante.setRutaArchivo(image.rutaArchivo());
        comprobante.setNombreArchivo(image.nombreArchivo());
        comprobante.setTipoContenido(image.tipoContenido());
        comprobante.setFechaRegistro(fechaRegistro);
        return comprobante;
    }

    public PagoComprobanteResponse toResponse(PagoComprobanteEntity comprobante) {
        return new PagoComprobanteResponse(comprobante.getId(), comprobante.getPago().getCodpag(),
                comprobante.getNombreArchivo(), comprobante.getTipoContenido(), comprobante.getFechaRegistro());
    }
}
