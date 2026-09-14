package com.orman.backend.property.dto.response;

import java.math.BigDecimal;

public record PropiedadResponse(Integer codprop, String nombre, String tipo, String direccion, String ciudad,
                                String referencia, BigDecimal latitud, BigDecimal longitud, String portadaUrl,
                                Integer codperPropietaria, BigDecimal inversionInicial, Short estado,
                                long cantidadUnidades, long unidadesHabilitadas, long unidadesOcupadas,
                                BigDecimal ocupacion, boolean tienePortada) {

    private static final BigDecimal OCUPACION_CERO = BigDecimal.ZERO.setScale(2);

    public PropiedadResponse(Integer codprop, String nombre, String tipo, String direccion, String ciudad,
                             String referencia, BigDecimal latitud, BigDecimal longitud, String portadaUrl,
                             Integer codperPropietaria, BigDecimal inversionInicial, Short estado) {
        this(codprop, nombre, tipo, direccion, ciudad, referencia, latitud, longitud, portadaUrl,
                codperPropietaria, inversionInicial, estado, 0, 0, 0, OCUPACION_CERO, false);
    }

    public PropiedadResponse(Integer codprop, String nombre, String tipo, String direccion, String ciudad,
                             String referencia, BigDecimal latitud, BigDecimal longitud, String portadaUrl,
                             Integer codperPropietaria, BigDecimal inversionInicial, Short estado,
                             long cantidadUnidades) {
        this(codprop, nombre, tipo, direccion, ciudad, referencia, latitud, longitud, portadaUrl,
                codperPropietaria, inversionInicial, estado, cantidadUnidades, 0, 0, OCUPACION_CERO, false);
    }

    public PropiedadResponse(Integer codprop, String nombre, String tipo, String direccion, String ciudad,
                             String referencia, BigDecimal latitud, BigDecimal longitud, String portadaUrl,
                             Integer codperPropietaria, BigDecimal inversionInicial, Short estado,
                             long cantidadUnidades, long unidadesHabilitadas, long unidadesOcupadas,
                             BigDecimal ocupacion) {
        this(codprop, nombre, tipo, direccion, ciudad, referencia, latitud, longitud, portadaUrl,
                codperPropietaria, inversionInicial, estado, cantidadUnidades, unidadesHabilitadas,
                unidadesOcupadas, ocupacion, false);
    }

    public PropiedadResponse {
        ocupacion = ocupacion == null ? OCUPACION_CERO : ocupacion;
    }
}
