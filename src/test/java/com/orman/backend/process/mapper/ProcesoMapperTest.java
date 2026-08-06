package com.orman.backend.process.mapper;

import com.orman.backend.process.dto.request.CreateProcesoRequest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcesoMapperTest {
    private final ProcesoMapper mapper = new ProcesoMapper();
    @Test
    void normalizesNameAndTrimsLinkWithoutInterpretingItAsAuthority() {
        var entity = mapper.toEntity(new CreateProcesoRequest(" listar personas ", " personas/listar ", null));
        assertThat(entity.getNombre()).isEqualTo("LISTAR PERSONAS");
        assertThat(entity.getEnlace()).isEqualTo("personas/listar");
        assertThat(entity.getEstado()).isNull();
    }
}
