package com.orman.backend.property.mapper;

import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import org.springframework.stereotype.Component;

@Component
public class UnidadMapper {

    public UnidadEntity toEntity(UnidadRequest request, PropiedadEntity propiedad) {
        UnidadEntity unidad = new UnidadEntity();
        apply(unidad, request, propiedad);
        return unidad;
    }

    public void update(UnidadEntity unidad, UnidadRequest request) {
        apply(unidad, request, unidad.getPropiedad());
    }

    public UnidadResponse toResponse(UnidadEntity unidad) {
        return new UnidadResponse(unidad.getCoduni(), unidad.getPropiedad().getCodprop(), unidad.getNombre(),
                unidad.getTipoUnidad(), unidad.getDescripcion(), unidad.getArea(), unidad.getDormitorios(),
                unidad.getBanos(), unidad.getPiso(), unidad.getUbicacionInterna(), unidad.getPrecioBase(),
                unidad.getEstadoOperativo());
    }

    private void apply(UnidadEntity unidad, UnidadRequest request, PropiedadEntity propiedad) {
        unidad.setPropiedad(propiedad);
        unidad.setNombre(trim(request.nombre()));
        unidad.setTipoUnidad(trim(request.tipoUnidad()));
        unidad.setDescripcion(blankToNull(request.descripcion()));
        unidad.setArea(request.area());
        unidad.setDormitorios(request.dormitorios());
        unidad.setBanos(request.banos());
        unidad.setPiso(request.piso());
        unidad.setUbicacionInterna(blankToNull(request.ubicacionInterna()));
        unidad.setPrecioBase(request.precioBase());
        unidad.setEstadoOperativo(request.estadoOperativo());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
