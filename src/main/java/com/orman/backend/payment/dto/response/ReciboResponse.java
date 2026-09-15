package com.orman.backend.payment.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

public record ReciboResponse(Integer codrec, Integer codpag, Integer codcuo, LocalDate periodo,
                             BigDecimal monto, String moneda, String metodo, LocalDateTime fechaPago,
                             LocalDateTime fechaEmision) {
}
