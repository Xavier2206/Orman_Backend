package com.orman.backend.contract.dto.response;

public record ContratoUnidadResponse(
        Integer coduni,
        String nombre,
        String tipoUnidad,
        String descripcion,
        Integer piso
) {
}
