package com.orman.backend.person.dto;

import java.time.OffsetDateTime;

public record PersonaResponse(Integer codper, String ci, String nombre, String ap, String am,
                              Character genero, Short estado, String correo, String telefono,
                              Character tipoPersona, String foto, OffsetDateTime fechaRegistro,
                              PersonaUsuarioResponse usuario, PersonaActionsResponse acciones) {
}
