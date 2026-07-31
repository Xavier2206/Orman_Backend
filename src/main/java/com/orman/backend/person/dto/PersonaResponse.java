package com.orman.backend.person.dto;

import java.time.LocalDateTime;

public record PersonaResponse(Integer codper, String ci, String nombre, String ap, String am,
                              Character genero, Short estado, String correo, String telefono,
                              Character tipoPersona, String foto, LocalDateTime fechaRegistro) {
}
