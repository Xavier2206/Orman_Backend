package com.orman.backend.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ContratoResponse(Integer codcon, Integer coduni, Integer codperInquilino, LocalDate fechaInicio,
                               LocalDate fechaFin, BigDecimal montoMensual, String moneda, BigDecimal garantia,
                               String estado, OffsetDateTime fechaRegistro, LocalDate fechaRescision,
                               String motivoRescision, ContratoInquilinoResponse inquilino,
                               ContratoUnidadResponse unidad, ContratoPropiedadResponse propiedad,
                               ContratoCuotasResumenResponse cuotas) {

    public ContratoResponse(Integer codcon, Integer coduni, Integer codperInquilino, LocalDate fechaInicio,
                            LocalDate fechaFin, BigDecimal montoMensual, String moneda, BigDecimal garantia,
                            String estado, OffsetDateTime fechaRegistro, LocalDate fechaRescision,
                            String motivoRescision) {
        this(codcon, coduni, codperInquilino, fechaInicio, fechaFin, montoMensual, moneda, garantia, estado,
                fechaRegistro, fechaRescision, motivoRescision, null, null, null, null);
    }
}
