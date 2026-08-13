package com.orman.backend.auth.repository;

/**
 * Proyección interna de la navegación vigente de un usuario autenticado.
 */
public record AuthContextNavigationRow(Integer codr, String rolNombre, Integer codm, String menuNombre,
        String menuIcono, Integer codp, String procesoNombre, String procesoEnlace) {
}
