package com.orman.backend.property.mapper;

import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadFotoMetadataRequest;
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
        foto.setUrl(request.url().trim());
        foto.setFotoRef(null);
        applyMetadata(foto, request.titulo(), request.ambiente(), request.orden());
    }

    public UnidadFotoEntity toInternalEntity(UnidadFotoMetadataRequest request, UnidadEntity unidad,
                                             String fotoRef) {
        UnidadFotoEntity foto = new UnidadFotoEntity();
        foto.setUnidad(unidad);
        foto.setUrl(null);
        foto.setFotoRef(fotoRef);
        foto.setPortada(false);
        updateMetadata(foto, request);
        return foto;
    }

    public void updateMetadata(UnidadFotoEntity foto, UnidadFotoMetadataRequest request) {
        applyMetadata(foto, request.titulo(), request.ambiente(), request.orden());
    }

    public UnidadFotoResponse toResponse(UnidadFotoEntity foto) {
        return new UnidadFotoResponse(foto.getId(), foto.getUnidad().getCoduni(), foto.getUrl(), foto.getTitulo(),
                foto.getAmbiente(), foto.getOrden(), foto.getPortada(), foto.getFotoRef() != null);
    }

    private void apply(UnidadFotoEntity foto, UnidadFotoRequest request) {
        foto.setUrl(request.url().trim());
        foto.setFotoRef(null);
        applyMetadata(foto, request.titulo(), request.ambiente(), request.orden());
    }

    private void applyMetadata(UnidadFotoEntity foto, String titulo, String ambiente, Integer orden) {
        foto.setTitulo(blankToNull(titulo));
        foto.setAmbiente(blankToNull(ambiente));
        foto.setOrden(orden);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
