package com.orman.backend.person.dto;

/** Minimal, non-sensitive representation of the Usuario linked to a Persona. */
public record PersonaUsuarioResponse(String login, Short estado) {
}
