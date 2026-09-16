package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;

public record ContratoCuotasResumenResponse(
        long totalCuotas,
        long cuotasPagadas,
        long cuotasPendientes,
        BigDecimal saldoPendiente
) {
}
