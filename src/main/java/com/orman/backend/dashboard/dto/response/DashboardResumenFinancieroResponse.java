package com.orman.backend.dashboard.dto.response;

import java.util.List;

public record DashboardResumenFinancieroResponse(String moneda,
                                                 DashboardResumenGeneralResponse resumenGeneral,
                                                 List<DashboardPropiedadResponse> propiedades,
                                                 List<DashboardIngresoMensualResponse> ingresosConfirmadosPorMes) {
}
