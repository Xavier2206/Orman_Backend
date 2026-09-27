package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InquilinoContratoResponse(Integer codcon, String estado, LocalDate fechaInicio, LocalDate fechaFin,
                                        LocalDate fechaRescision, String motivoRescision,
                                        BigDecimal montoMensual, String moneda, BigDecimal garantia,
                                        Integer codprop, String nombrePropiedad,
                                        Integer coduni, String nombreUnidad) {
}
