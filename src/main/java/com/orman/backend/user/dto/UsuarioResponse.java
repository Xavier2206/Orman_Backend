package com.orman.backend.user.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(String login, Short estado, Integer codper, LocalDateTime fechaCreacion,
                              LocalDateTime ultimoAcceso, String nombre, String ap, String am) {

    public UsuarioResponse(String login, Short estado, Integer codper, LocalDateTime fechaCreacion,
                           LocalDateTime ultimoAcceso) {
        this(login, estado, codper, fechaCreacion, ultimoAcceso, null, null, null);
    }
}
