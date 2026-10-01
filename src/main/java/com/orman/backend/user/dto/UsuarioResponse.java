package com.orman.backend.user.dto;

import java.time.OffsetDateTime;

public record UsuarioResponse(String login, Short estado, Integer codper, OffsetDateTime fechaCreacion,
                              OffsetDateTime ultimoAcceso, String nombre, String ap, String am) {

    public UsuarioResponse(String login, Short estado, Integer codper, OffsetDateTime fechaCreacion,
                           OffsetDateTime ultimoAcceso) {
        this(login, estado, codper, fechaCreacion, ultimoAcceso, null, null, null);
    }
}
