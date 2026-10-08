package com.orman.backend.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardPropiedadResponse(Integer codprop,
                                         String nombre,
                                         BigDecimal inversionInicial,
                                         BigDecimal ingresosConfirmadosAcumulados,
                                         BigDecimal porcentajeRecuperacion,
                                         List<DashboardIngresoMensualResponse> ingresosConfirmadosPorMes) {
}
