package com.orman.backend.property.dto.response;

public record UnidadFotoResponse(Integer id, Integer coduni, String url, String titulo, String ambiente,
                                 Integer orden, Boolean portada, boolean tieneArchivo) {
}
