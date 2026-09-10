package com.orman.backend.payment.dto.response;

import java.time.LocalDateTime;

public record ReciboResponse(Integer codrec, Integer codpag, LocalDateTime fechaEmision) {
}
