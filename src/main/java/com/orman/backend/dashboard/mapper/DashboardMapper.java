package com.orman.backend.dashboard.mapper;

import com.orman.backend.dashboard.dto.response.DashboardIngresoMensualResponse;
import com.orman.backend.dashboard.dto.response.DashboardPropiedadResponse;
import com.orman.backend.dashboard.dto.response.DashboardResumenFinancieroResponse;
import com.orman.backend.dashboard.dto.response.DashboardResumenGeneralResponse;
import com.orman.backend.dashboard.repository.DashboardPropiedadProjection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    public DashboardPropiedadResponse toPropiedad(DashboardPropiedadProjection propiedad,
                                                   BigDecimal ingresos,
                                                   BigDecimal porcentaje,
                                                   List<DashboardIngresoMensualResponse> ingresosMensuales) {
        return new DashboardPropiedadResponse(propiedad.getCodprop(), propiedad.getNombre(),
                propiedad.getInversionInicial(), ingresos, porcentaje, List.copyOf(ingresosMensuales));
    }

    public DashboardResumenFinancieroResponse toResponse(long cantidadPropiedades,
                                                         BigDecimal inversionTotal,
                                                         BigDecimal ingresosTotal,
                                                         BigDecimal porcentajeGlobal,
                                                         List<DashboardPropiedadResponse> propiedades,
                                                         List<DashboardIngresoMensualResponse> ingresosMensuales) {
        DashboardResumenGeneralResponse general = new DashboardResumenGeneralResponse(cantidadPropiedades,
                inversionTotal, ingresosTotal, porcentajeGlobal);
        return new DashboardResumenFinancieroResponse("BOB", general, List.copyOf(propiedades),
                List.copyOf(ingresosMensuales));
    }
}
