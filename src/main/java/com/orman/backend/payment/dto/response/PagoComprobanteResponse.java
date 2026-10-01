package com.orman.backend.payment.dto.response;

import java.time.OffsetDateTime;

public record PagoComprobanteResponse(Integer id, Integer codpag, String nombreArchivo,
                                      String tipoContenido, OffsetDateTime fechaRegistro) {
}
