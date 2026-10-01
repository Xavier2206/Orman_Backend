package com.orman.backend.payment.mapper;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.entity.QrCobroEntity;
import org.springframework.stereotype.Component;

@Component
public class QrCobroMapper {

    public QrCobroResponse toResponse(QrCobroEntity qr) {
        return new QrCobroResponse(qr.getCodqr(), qr.getFechaInicio(), qr.getFechaFin(), qr.getEstado(),
                qr.getRutaArchivo() != null, qr.getNombreArchivo(), qr.getTipoContenido(),
                OrmanTimeConfig.ormanLocalToOffset(qr.getFechaRegistro()));
    }
}
