package com.orman.backend.role.dto.response;

public record RolMeResponse(
        Integer codr, String nombreRol, Short estadoRol,
        Integer codm, String nombreMenu, Short estadoMenu) {
}
