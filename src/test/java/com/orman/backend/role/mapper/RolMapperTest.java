package com.orman.backend.role.mapper;

import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RolMapperTest {

    private final RolMapper mapper = new RolMapper();

    @Test
    void normalizesNombreAndKeepsDatabaseDefaultAvailable() {
        Rol rol = mapper.toEntity(new CreateRolRequest(" administrador ", null));

        assertThat(rol.getNombre()).isEqualTo("ADMINISTRADOR");
        assertThat(rol.getEstado()).isNull();
    }

    @Test
    void updatesOnlyNombreAndMapsResponse() {
        Rol rol = rol(7, "OPERADOR", (short) 1);

        mapper.update(rol, new UpdateRolRequest(" supervisor "));

        assertThat(rol.getNombre()).isEqualTo("SUPERVISOR");
        assertThat(rol.getEstado()).isEqualTo((short) 1);
        assertThat(mapper.toResponse(rol)).isEqualTo(new RolResponse(7, "SUPERVISOR", (short) 1));
    }

    private Rol rol(Integer codr, String nombre, short estado) {
        Rol rol = new Rol();
        ReflectionTestUtils.setField(rol, "codr", codr);
        rol.setNombre(nombre);
        rol.setEstado(estado);
        return rol;
    }
}
