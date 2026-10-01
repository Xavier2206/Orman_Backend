package com.orman.backend.payment.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoResponse(Integer codpag, Integer codcuo, Integer codqr, BigDecimal monto, String metodo,
                           OffsetDateTime fechaPago, OffsetDateTime fechaRegistro,
                           String estado, String origenRegistro, String registradoPor, String revisadoPor,
                           OffsetDateTime fechaRevision, String motivoRechazo, String motivoAnulacion) {
}
