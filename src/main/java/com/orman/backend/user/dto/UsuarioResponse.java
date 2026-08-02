package com.orman.backend.user.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(String login, Short estado, Integer codper, LocalDateTime fechaCreacion,
                              LocalDateTime ultimoAcceso) {
}
