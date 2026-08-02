package com.orman.backend.role.dto.response;

import java.time.LocalDateTime;

public record RolUsuResponse(String login, Integer codr, String nombreRol, LocalDateTime fechaAsignacion) {
}
