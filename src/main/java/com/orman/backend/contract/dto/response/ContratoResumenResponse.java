package com.orman.backend.contract.dto.response;

public record ContratoResumenResponse(
        long vigentes,
        long programados,
        long finalizados,
        long rescindidos
) {
}
