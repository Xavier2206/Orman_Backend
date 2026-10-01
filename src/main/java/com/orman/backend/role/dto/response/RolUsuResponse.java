package com.orman.backend.role.dto.response;

import java.time.OffsetDateTime;

public record RolUsuResponse(String login, Integer codr, String nombreRol, OffsetDateTime fechaAsignacion) {
}
