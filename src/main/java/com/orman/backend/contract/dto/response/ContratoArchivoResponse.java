package com.orman.backend.contract.dto.response;

public record ContratoArchivoResponse(Integer id, Integer codcon, String url, String nombreArchivo,
                                      String tipoContenido, Integer orden) {
}
