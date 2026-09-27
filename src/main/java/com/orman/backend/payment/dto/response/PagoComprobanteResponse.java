package com.orman.backend.payment.dto.response;

import java.time.LocalDateTime;

public record PagoComprobanteResponse(Integer id, Integer codpag, String nombreArchivo,
                                      String tipoContenido, LocalDateTime fechaRegistro) {
}
