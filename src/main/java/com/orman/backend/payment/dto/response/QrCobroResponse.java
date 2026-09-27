package com.orman.backend.payment.dto.response;

import com.orman.backend.payment.entity.QrCobroEstado;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record QrCobroResponse(Integer codqr, LocalDate fechaInicio, LocalDate fechaFin, QrCobroEstado estado,
                              boolean tieneImagen, String nombreArchivo, String tipoContenido,
                              LocalDateTime fechaRegistro) {
}
