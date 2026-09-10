package com.orman.backend.payment.dto.response;

import java.time.LocalDateTime;

public record PagoComprobanteResponse(Integer id, Integer codpag, String url, String nombreArchivo,
                                      String tipoContenido, Integer orden, LocalDateTime fechaRegistro) {
}
