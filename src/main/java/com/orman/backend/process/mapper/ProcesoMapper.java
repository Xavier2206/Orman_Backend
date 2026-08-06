package com.orman.backend.process.mapper;

import com.orman.backend.process.dto.request.CreateProcesoRequest;
import com.orman.backend.process.dto.request.UpdateProcesoRequest;
import com.orman.backend.process.dto.response.ProcesoResponse;
import com.orman.backend.process.entity.Proceso;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ProcesoMapper {

    public Proceso toEntity(CreateProcesoRequest request) {
        return new Proceso(normalizeNombre(request.nombre()), normalizeEnlace(request.enlace()), request.estado());
    }

    public void update(Proceso proceso, UpdateProcesoRequest request) {
        proceso.setNombre(normalizeNombre(request.nombre()));
        proceso.setEnlace(normalizeEnlace(request.enlace()));
    }

    public ProcesoResponse toResponse(Proceso proceso) {
        return new ProcesoResponse(proceso.getCodp(), proceso.getNombre(), proceso.getEnlace(), proceso.getEstado());
    }

    public String normalizeNombre(String nombre) {
        return nombre == null ? null : nombre.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizeEnlace(String enlace) {
        return enlace == null ? null : enlace.trim();
    }
}
