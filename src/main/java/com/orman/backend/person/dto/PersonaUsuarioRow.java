package com.orman.backend.person.dto;

/** Internal projection used to enrich a page without loading Usuario entities per Persona. */
public record PersonaUsuarioRow(Integer codper, String login, Short estado) {
}
