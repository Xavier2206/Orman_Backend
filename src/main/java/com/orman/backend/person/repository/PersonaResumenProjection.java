package com.orman.backend.person.repository;

/** Internal projection for the global Persona aggregate. */
public interface PersonaResumenProjection {

    Long getTotalPersonas();

    Long getActivas();

    Long getInactivas();

    Long getConUsuario();
}
