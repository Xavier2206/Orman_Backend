package com.orman.backend.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(Integer codpag, Integer codcuo, Integer codcta, BigDecimal monto, String metodo,
                           String referenciaExterna, LocalDateTime fechaPago, LocalDateTime fechaRegistro,
                           String estado, String origenRegistro, String registradoPor, String revisadoPor,
                           LocalDateTime fechaRevision, String motivoRechazo, String motivoAnulacion) {
}
