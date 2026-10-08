package com.orman.backend.dashboard.dto.response;

import java.math.BigDecimal;

public record DashboardResumenGeneralResponse(long cantidadPropiedades,
                                              BigDecimal inversionInicialTotal,
                                              BigDecimal ingresosConfirmadosAcumulados,
                                              BigDecimal porcentajeRecuperacion) {
}
