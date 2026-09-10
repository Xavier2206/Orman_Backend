package com.orman.backend.property.mapper;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.entity.UnidadFotoEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapperTest {

    private final PropiedadMapper propiedadMapper = new PropiedadMapper();
    private final UnidadMapper unidadMapper = new UnidadMapper();
    private final UnidadFotoMapper unidadFotoMapper = new UnidadFotoMapper();

    @Test
    void mapsAndNormalizesPropertyUnitAndPhotoWithoutExposingEntities() {
        Persona persona = persona(4);
        PropiedadEntity propiedad = propiedadMapper.toEntity(propiedadRequest(), persona);
        ReflectionTestUtils.setField(propiedad, "codprop", 9);

        assertThat(propiedad.getNombre()).isEqualTo("Edificio Central");
        assertThat(propiedad.getTipo()).isEqualTo("EDIFICIO");
        assertThat(propiedad.getReferencia()).isNull();
        assertThat(propiedadMapper.toResponse(propiedad).codperPropietaria()).isEqualTo(4);

        UnidadEntity unidad = unidadMapper.toEntity(unidadRequest(), propiedad);
        ReflectionTestUtils.setField(unidad, "coduni", 15);
        assertThat(unidad.getDescripcion()).isNull();
        assertThat(unidadMapper.toResponse(unidad).codprop()).isEqualTo(9);

        UnidadFotoEntity foto = unidadFotoMapper.toEntity(fotoRequest(), unidad);
        ReflectionTestUtils.setField(foto, "id", 22);
        assertThat(foto.getPortada()).isFalse();
        assertThat(foto.getTitulo()).isNull();
        assertThat(unidadFotoMapper.toResponse(foto).coduni()).isEqualTo(15);
    }

    private PropiedadRequest propiedadRequest() {
        return new PropiedadRequest(" Edificio Central ", " edificio ", " Av. Principal 123 ", " La Paz ", " ",
                new BigDecimal("-16.500000"), new BigDecimal("-68.150000"), "https://example.test/portada.jpg",
                4, new BigDecimal("100000.00"), (short) 1);
    }

    private UnidadRequest unidadRequest() {
        return new UnidadRequest(" A-101 ", "Departamento", " ", new BigDecimal("45.50"), (short) 1,
                (short) 1, 1, " Torre A ", new BigDecimal("2500.00"), (short) 1);
    }

    private UnidadFotoRequest fotoRequest() {
        return new UnidadFotoRequest("https://example.test/a-101.jpg", " ", " Sala ", 0);
    }

    private Persona persona(Integer codper) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        return persona;
    }
}
