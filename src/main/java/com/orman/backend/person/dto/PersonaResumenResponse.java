package com.orman.backend.person.dto;

public record PersonaResumenResponse(
        long totalPersonas,
        long activas,
        long inactivas,
        long conUsuario) {
}
