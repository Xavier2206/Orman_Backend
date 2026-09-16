package com.orman.backend.contract.dto.response;

import java.time.LocalDateTime;

public record ContratoArchivoResponse(Integer codarc, Integer codcon, String nombreArchivo,
                                      String tipoContenido, Long tamanoOriginal, Long tamanoFinal,
                                      LocalDateTime fechaSubida, String subidoPor, Integer orden,
                                      boolean almacenadoInternamente) {
}
