package com.orman.backend.property.dto.response;

import java.math.BigDecimal;

public record UnidadResponse(Integer coduni, Integer codprop, String nombre, String tipoUnidad, String descripcion,
                             BigDecimal area, Short dormitorios, Short banos, Integer piso, String ubicacionInterna,
                             BigDecimal precioBase, Short estadoOperativo, boolean disponibleParaContrato) {
}
