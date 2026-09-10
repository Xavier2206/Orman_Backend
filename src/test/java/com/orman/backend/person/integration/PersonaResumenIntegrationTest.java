package com.orman.backend.person.integration;

import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.PersonaResumenResponse;
import com.orman.backend.person.service.PersonaService;
import com.orman.backend.user.dto.CreateUsuarioRequest;
import com.orman.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class PersonaResumenIntegrationTest {

    private static final String PASSWORD = "clave-ficticia";

    @Autowired private PersonaService personaService;
    @Autowired private UsuarioService usuarioService;

    @Test
    void calculatesGlobalCountsAndIncludesActiveAndInactiveUsers() {
        PersonaResumenResponse before = personaService.resumen();

        PersonaResponse activeWithActiveUser = createPersona("RESUMEN-ACTIVA-001", "1");
        PersonaResponse activeWithInactiveUser = createPersona("RESUMEN-ACTIVA-002", "1");
        PersonaResponse inactiveWithActiveUser = createPersona("RESUMEN-INACTIVA-001", "0");
        createPersona("RESUMEN-INACTIVA-002", "0");

        createUsuario("resumen.active", (short) 1, activeWithActiveUser.codper());
        createUsuario("resumen.inactive", (short) 0, activeWithInactiveUser.codper());
        createUsuario("resumen.inactive.person", (short) 1, inactiveWithActiveUser.codper());

        PersonaResumenResponse after = personaService.resumen();

        assertThat(after.totalPersonas()).isEqualTo(before.totalPersonas() + 4);
        assertThat(after.activas()).isEqualTo(before.activas() + 2);
        assertThat(after.inactivas()).isEqualTo(before.inactivas() + 2);
        assertThat(after.conUsuario()).isEqualTo(before.conUsuario() + 3);
    }

    private PersonaResponse createPersona(String ci, String estado) {
        return personaService.create(new CreatePersonaRequest(ci, "Persona resumen", null, null,
                "F", estado, ci.toLowerCase() + "@example.test", "70000000", "A", null));
    }

    private void createUsuario(String login, short estado, Integer codper) {
        usuarioService.create(new CreateUsuarioRequest(login, PASSWORD, estado, codper));
    }
}
