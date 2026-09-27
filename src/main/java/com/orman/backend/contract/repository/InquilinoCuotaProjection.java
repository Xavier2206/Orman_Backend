package com.orman.backend.contract.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InquilinoCuotaProjection {

    Integer getCodcuo();

    Integer getCodcon();

    LocalDate getPeriodo();

    LocalDate getFechaVencimiento();

    BigDecimal getMonto();

    BigDecimal getMontoConfirmado();

    BigDecimal getMontoPendienteRevision();

    BigDecimal getSaldo();

    String getEstado();
}
