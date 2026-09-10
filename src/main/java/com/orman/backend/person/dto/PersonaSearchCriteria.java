package com.orman.backend.person.dto;

import com.orman.backend.person.exception.InvalidPersonaFilterException;
import java.util.Locale;

public record PersonaSearchCriteria(String q, Character tipoPersona, Short estado) {

    public static PersonaSearchCriteria from(String q, String tipoPersona, String estado) {
        return new PersonaSearchCriteria(normalizeQuery(q), parseTipoPersona(tipoPersona), parseEstado(estado));
    }

    private static String normalizeQuery(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static Character parseTipoPersona(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 1 || (normalized.charAt(0) != 'A' && normalized.charAt(0) != 'I')) {
            throw new InvalidPersonaFilterException("tipoPersona", "El tipo de persona debe ser A o I.");
        }
        return normalized.charAt(0);
    }

    private static Short parseEstado(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        if (!"0".equals(normalized) && !"1".equals(normalized)) {
            throw new InvalidPersonaFilterException("estado", "El estado debe ser 0 o 1.");
        }
        return Short.valueOf(normalized);
    }
}
