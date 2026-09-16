package com.orman.backend.contract.repository;

import java.math.BigDecimal;

public interface ContratoCuotasResumenProjection {

    Integer getCodcon();

    long getTotalCuotas();

    long getCuotasPagadas();

    long getCuotasPendientes();

    BigDecimal getSaldoPendiente();
}
