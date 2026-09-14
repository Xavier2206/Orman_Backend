package com.orman.backend.property.mapper;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PropiedadMapper {

    public PropiedadEntity toEntity(PropiedadRequest request, Persona propietaria) {
        PropiedadEntity propiedad = new PropiedadEntity();
        apply(propiedad, request, propietaria);
        return propiedad;
    }

    public void update(PropiedadEntity propiedad, PropiedadRequest request, Persona propietaria) {
        apply(propiedad, request, propietaria);
    }

    public PropiedadResponse toResponse(PropiedadEntity propiedad) {
        return toResponse(propiedad, 0, 0, 0, BigDecimal.ZERO.setScale(2));
    }

    public PropiedadResponse toResponse(PropiedadEntity propiedad, long cantidadUnidades) {
        return toResponse(propiedad, cantidadUnidades, 0, 0, BigDecimal.ZERO.setScale(2));
    }

    public PropiedadResponse toResponse(PropiedadEntity propiedad, long cantidadUnidades, long unidadesHabilitadas,
                                        long unidadesOcupadas, BigDecimal ocupacion) {
        return new PropiedadResponse(propiedad.getCodprop(), propiedad.getNombre(), propiedad.getTipo(),
                propiedad.getDireccion(), propiedad.getCiudad(), propiedad.getReferencia(), propiedad.getLatitud(),
                propiedad.getLongitud(), propiedad.getPortadaUrl(), propiedad.getPropietaria().getCodper(),
                propiedad.getInversionInicial(), propiedad.getEstado(), cantidadUnidades, unidadesHabilitadas,
                unidadesOcupadas, ocupacion, propiedad.getPortadaRef() != null);
    }

    private void apply(PropiedadEntity propiedad, PropiedadRequest request, Persona propietaria) {
        propiedad.setNombre(trim(request.nombre()));
        propiedad.setTipo(upper(request.tipo()));
        propiedad.setDireccion(trim(request.direccion()));
        propiedad.setCiudad(trim(request.ciudad()));
        propiedad.setReferencia(blankToNull(request.referencia()));
        propiedad.setLatitud(request.latitud());
        propiedad.setLongitud(request.longitud());
        propiedad.setPortadaUrl(blankToNull(request.portadaUrl()));
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(request.inversionInicial());
        propiedad.setEstado(request.estado());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private String upper(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }
}
