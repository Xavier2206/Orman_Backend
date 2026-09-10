package com.orman.backend.person.dto;

import com.orman.backend.person.exception.InvalidPersonaFilterException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonaSearchCriteriaTest {

    @Test
    void normalizesOptionalFiltersAndBlankSearch() {
        PersonaSearchCriteria criteria = PersonaSearchCriteria.from("  Ortega ", " i ", " 0 ");
        assertThat(criteria.q()).isEqualTo("Ortega");
        assertThat(criteria.tipoPersona()).isEqualTo('I');
        assertThat(criteria.estado()).isEqualTo((short) 0);
        assertThat(PersonaSearchCriteria.from("   ", "", null))
                .isEqualTo(new PersonaSearchCriteria(null, null, null));
    }

    @Test
    void rejectsInvalidFiltersWithTheirField() {
        assertThatThrownBy(() -> PersonaSearchCriteria.from(null, "X", null))
                .isInstanceOf(InvalidPersonaFilterException.class)
                .hasMessage("El tipo de persona debe ser A o I.");
        assertThatThrownBy(() -> PersonaSearchCriteria.from(null, null, "2"))
                .isInstanceOf(InvalidPersonaFilterException.class)
                .hasMessage("El estado debe ser 0 o 1.");
    }
}
