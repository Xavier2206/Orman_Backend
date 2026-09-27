package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InquilinoCuotaResponse(Integer codcuo, Integer codcon, LocalDate periodo,
                                     LocalDate fechaVencimiento, BigDecimal monto,
                                     BigDecimal montoConfirmado, BigDecimal montoPendienteRevision,
                                     BigDecimal saldo, String estado, String situacionVencimiento) {
}
