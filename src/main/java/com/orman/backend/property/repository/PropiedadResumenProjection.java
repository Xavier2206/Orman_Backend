package com.orman.backend.property.repository;

import java.math.BigDecimal;

public interface PropiedadResumenProjection {

    BigDecimal getInversionTotal();

    long getPropiedadesActivas();

    long getCasasActivas();

    long getEdificiosActivos();

    long getUnidadesTotales();

    long getUnidadesHabilitadas();

    long getUnidadesNoHabilitadas();

    long getUnidadesOcupadas();
}
