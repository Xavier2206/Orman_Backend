package com.orman.backend.role.mapper;

import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import org.springframework.stereotype.Component;


@Component
public class RolMapper {

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.getCodr(), rol.getNombre(), rol.getEstado());
    }

}
