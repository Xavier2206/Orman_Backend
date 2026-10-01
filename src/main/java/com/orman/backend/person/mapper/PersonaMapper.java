package com.orman.backend.person.mapper;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.dto.PersonaActionsResponse;
import com.orman.backend.person.dto.PersonaUsuarioResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PersonaMapper {

    public Persona toEntity(CreatePersonaRequest request) {
        Persona persona = new Persona();
        apply(persona, request.ci(), request.nombre(), request.ap(), request.am(), request.genero(),
                request.estado(), request.correo(), request.telefono(), request.tipoPersona());
        persona.setFoto(blankToNull(request.foto()));
        return persona;
    }

    public void update(Persona persona, UpdatePersonaRequest request) {
        apply(persona, request.ci(), request.nombre(), request.ap(), request.am(), request.genero(),
                request.estado(), request.correo(), request.telefono(), request.tipoPersona());
    }

    public PersonaResponse toResponse(Persona persona) {
        return new PersonaResponse(persona.getCodper(),
                persona.getCi(),
                persona.getNombre(),
                persona.getAp(),
                persona.getAm(),
                persona.getGenero(),
                persona.getEstado(),
                persona.getCorreo(),
                persona.getTelefono(),
                persona.getTipoPersona(),
                persona.getFoto(),
                OrmanTimeConfig.ormanLocalToOffset(persona.getFechaRegistro()), null,
                new PersonaActionsResponse(false, false, false, false, false, false));
    }

    public PersonaResponse toResponse(Persona persona, PersonaUsuarioResponse usuario,
                                      PersonaActionsResponse acciones) {
        PersonaResponse base = toResponse(persona);
        return new PersonaResponse(base.codper(), base.ci(), base.nombre(), base.ap(), base.am(), base.genero(),
                base.estado(), base.correo(), base.telefono(), base.tipoPersona(), base.foto(),
                base.fechaRegistro(), usuario, acciones);
    }

    private void apply(Persona persona, String ci, String nombre, String ap, String am, String genero, String estado,
                       String correo, String telefono, String tipoPersona) {
        persona.setCi(trim(ci)); persona.setNombre(trim(nombre)); persona.setAp(blankToNull(ap)); persona.setAm(blankToNull(am));
        persona.setGenero(upper(genero)); persona.setEstado(estado == null ? null : Short.valueOf(estado));
        persona.setCorreo(blankToNull(correo)); persona.setTelefono(trim(telefono)); persona.setTipoPersona(upper(tipoPersona));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value); return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private Character upper(String value) {
        return trim(value).toUpperCase(Locale.ROOT).charAt(0);
    }
}
