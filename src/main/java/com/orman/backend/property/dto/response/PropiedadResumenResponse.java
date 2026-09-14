package com.orman.backend.property.dto.response;

import java.math.BigDecimal;

public record PropiedadResumenResponse(
        BigDecimal inversionTotal,
        long propiedadesActivas,
        long casasActivas,
        long edificiosActivos,
        long unidadesTotales,
        long unidadesHabilitadas,
        long unidadesNoHabilitadas,
        long unidadesOcupadas,
        BigDecimal ocupacionGlobal) {
}
