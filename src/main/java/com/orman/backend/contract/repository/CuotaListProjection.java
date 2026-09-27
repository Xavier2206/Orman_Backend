package com.orman.backend.contract.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaListProjection(
        Integer codcuo,
        Integer codcon,
        LocalDate periodo,
        LocalDate fechaVencimiento,
        Integer codperInquilino,
        String nombreCompleto,
        String ci,
        Integer codprop,
        String nombrePropiedad,
        Integer coduni,
        String nombreUnidad,
        BigDecimal monto,
        BigDecimal montoConfirmado,
        BigDecimal montoPendienteRevision,
        BigDecimal saldo,
        String estado) {
}
