package com.orman.backend.property.dto.response;

import java.math.BigDecimal;

public record PropiedadResponse(Integer codprop, String nombre, String tipo, String direccion, String ciudad,
                                String referencia, BigDecimal latitud, BigDecimal longitud, String portadaUrl,
                                Integer codperPropietaria, BigDecimal inversionInicial, Short estado) {
}
