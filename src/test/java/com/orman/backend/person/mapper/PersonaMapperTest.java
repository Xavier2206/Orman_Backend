package com.orman.backend.person.mapper;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.entity.Persona;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaMapperTest {

    private final PersonaMapper mapper = new PersonaMapper();

    @Test
    void convertsCreateRequestAndPreservesNullStatusForDatabaseDefault() {
        Persona persona = mapper.toEntity(new CreatePersonaRequest(" CI-001 ", " Nombre ", " ", " Apellido ",
                "f", null, " correo@example.test ", " 70000001 ", "a", " "));

        assertThat(persona.getCi()).isEqualTo("CI-001");
        assertThat(persona.getNombre()).isEqualTo("Nombre");
        assertThat(persona.getAp()).isNull();
        assertThat(persona.getAm()).isEqualTo("Apellido");
        assertThat(persona.getGenero()).isEqualTo('F');
        assertThat(persona.getEstado()).isNull();
        assertThat(persona.getCorreo()).isEqualTo("correo@example.test");
        assertThat(persona.getTelefono()).isEqualTo("70000001");
        assertThat(persona.getTipoPersona()).isEqualTo('A');
        assertThat(persona.getFoto()).isNull();
    }

    @Test
    void updatesOnlyEditableFieldsAndNormalizesApprovedValues() {
        Persona persona = persona(9, "CI-ANTERIOR", LocalDateTime.of(2026, 1, 1, 10, 0));

        mapper.update(persona, new UpdatePersonaRequest(" CI-002 ", " Nuevo nombre ", " ", " Materno ",
                "m", "0", " ", " 70000002 ", "i", " foto "));

        assertThat(persona.getCodper()).isEqualTo(9);
        assertThat(persona.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(persona.getCi()).isEqualTo("CI-002");
        assertThat(persona.getNombre()).isEqualTo("Nuevo nombre");
        assertThat(persona.getAp()).isNull();
        assertThat(persona.getAm()).isEqualTo("Materno");
        assertThat(persona.getGenero()).isEqualTo('M');
        assertThat(persona.getEstado()).isEqualTo((short) 0);
        assertThat(persona.getCorreo()).isNull();
        assertThat(persona.getTelefono()).isEqualTo("70000002");
        assertThat(persona.getTipoPersona()).isEqualTo('I');
        assertThat(persona.getFoto()).isEqualTo("foto");
    }

    @Test
    void convertsEntityToResponseWithoutAlteringData() {
        LocalDateTime fechaRegistro = LocalDateTime.of(2026, 2, 3, 4, 5);
        Persona persona = persona(10, "CI-003", fechaRegistro);
        persona.setEstado((short) 1);

        PersonaResponse response = mapper.toResponse(persona);

        assertThat(response).isEqualTo(new PersonaResponse(10, "CI-003", "Nombre", "Paterno", "Materno",
                'F', (short) 1, "correo@example.test", "70000000", 'A', "foto", fechaRegistro));
    }

    private Persona persona(Integer codper, String ci, LocalDateTime fechaRegistro) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        persona.setCi(ci);
        persona.setNombre("Nombre");
        persona.setAp("Paterno");
        persona.setAm("Materno");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo("correo@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        persona.setFoto("foto");
        persona.setFechaRegistro(fechaRegistro);
        return persona;
    }
}
