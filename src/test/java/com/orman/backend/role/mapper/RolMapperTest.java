package com.orman.backend.role.mapper;

import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RolMapperTest {

    @Test
    void mapsFixedRoleToResponse() {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "codr", 7);
        rol.setNombre("INQUILINO");
        rol.setEstado((short) 1);
        assertThat(new RolMapper().toResponse(rol)).isEqualTo(new RolResponse(7, "INQUILINO", (short) 1));
    }
}
