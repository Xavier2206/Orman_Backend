package com.orman.backend.contract.dto.response;

import java.time.OffsetDateTime;

public record ContratoArchivoResponse(Integer codarc, Integer codcon, String nombreArchivo,
                                      String tipoContenido, Long tamanoOriginal, Long tamanoFinal,
                                      OffsetDateTime fechaSubida, String subidoPor, Integer orden,
                                      boolean almacenadoInternamente) {
}
