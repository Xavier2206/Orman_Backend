package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContratoResponse(Integer codcon, Integer coduni, Integer codperInquilino, LocalDate fechaInicio,
                               LocalDate fechaFin, BigDecimal montoMensual, BigDecimal garantia, String estado,
                               LocalDateTime fechaConfirmacion, LocalDate fechaRescision, String motivoRescision,
                               Integer codconOrigen) {
}
