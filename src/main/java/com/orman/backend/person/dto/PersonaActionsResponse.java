package com.orman.backend.person.dto;

/** Presentation capabilities. Every operation remains authorized by the backend. */
public record PersonaActionsResponse(boolean puedeEditar, boolean puedeDesactivar, boolean puedeActivar,
                                     boolean puedeEliminar, boolean puedeCrearUsuario,
                                     boolean puedeCambiarPassword) {
}
