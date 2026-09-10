package com.orman.backend.property.mapper;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.entity.UnidadFotoEntity;
import org.springframework.stereotype.Component;

@Component
public class UnidadFotoMapper {

    public UnidadFotoEntity toEntity(UnidadFotoRequest request, UnidadEntity unidad) {
        UnidadFotoEntity foto = new UnidadFotoEntity();
        foto.setUnidad(unidad);
        foto.setPortada(false);
        apply(foto, request);
        return foto;
    }

    public void update(UnidadFotoEntity foto, UnidadFotoRequest request) {
        apply(foto, request);
    }

    public UnidadFotoResponse toResponse(UnidadFotoEntity foto) {
        return new UnidadFotoResponse(foto.getId(), foto.getUnidad().getCoduni(), foto.getUrl(), foto.getTitulo(),
                foto.getAmbiente(), foto.getOrden(), foto.getPortada());
    }

    private void apply(UnidadFotoEntity foto, UnidadFotoRequest request) {
        foto.setUrl(request.url().trim());
        foto.setTitulo(blankToNull(request.titulo()));
        foto.setAmbiente(blankToNull(request.ambiente()));
        foto.setOrden(request.orden());
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
