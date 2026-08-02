package com.orman.backend.role.mapper;

import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RolMapper {

    public Rol toEntity(CreateRolRequest request) {
        Rol rol = new Rol();
        rol.setNombre(normalizeNombre(request.nombre()));
        rol.setEstado(request.estado());
        return rol;
    }

    public void update(Rol rol, UpdateRolRequest request) {
        rol.setNombre(normalizeNombre(request.nombre()));
    }

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.getCodr(), rol.getNombre(), rol.getEstado());
    }

    public String normalizeNombre(String nombre) {
        return nombre == null ? null : nombre.trim().toUpperCase(Locale.ROOT);
    }
}
