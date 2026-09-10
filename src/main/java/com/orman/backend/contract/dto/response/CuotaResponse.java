package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(Integer codcuo, Integer codcon, LocalDate periodo, LocalDate fechaVencimiento,
                            BigDecimal monto, String estado) {
}
