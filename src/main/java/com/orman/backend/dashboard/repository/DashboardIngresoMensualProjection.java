package com.orman.backend.dashboard.repository;

import java.math.BigDecimal;

public interface DashboardIngresoMensualProjection {

    Integer getCodprop();

    Integer getAnio();

    Integer getMes();

    BigDecimal getTotal();
}
