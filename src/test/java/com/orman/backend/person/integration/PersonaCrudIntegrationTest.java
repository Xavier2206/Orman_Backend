package com.orman.backend.person.integration;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.dto.CreatePersonaRequest;
import com.orman.backend.person.dto.PersonaResponse;
import com.orman.backend.person.dto.PersonaSearchCriteria;
import com.orman.backend.person.dto.UpdatePersonaRequest;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.person.service.PersonaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class PersonaCrudIntegrationTest {

    @Autowired private PersonaService personaService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void executesCrudLifecycleIncludingStatusChangesAndPhysicalDeletion() {
        PersonaResponse created = personaService.create(createRequest("TEST-CRUD-001", null));

        assertThat(created.codper()).isNotNull();
        assertThat(created.estado()).isEqualTo((short) 1);
        assertThat(created.fechaRegistro()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT fecha_registro FROM personas WHERE codper = ?", Object.class,
                created.codper())).isNotNull();
        assertThat(personaService.get(created.codper())).isEqualTo(created);
        assertThat(personaService.list(org.springframework.data.domain.PageRequest.of(0, 20)).content())
                .extracting(PersonaResponse::codper).contains(created.codper());

        PersonaResponse updated = personaService.update(created.codper(), updateRequest("TEST-CRUD-001", "1"));
        assertThat(updated.nombre()).isEqualTo("Nombre actualizado");
        assertThat(updated.ap()).isNull();
        assertThat(updated.estado()).isEqualTo((short) 1);

        assertThat(personaService.deactivate(created.codper()).estado()).isEqualTo((short) 0);
        assertThat(personaService.get(created.codper()).estado()).isEqualTo((short) 0);
        assertThat(personaService.activate(created.codper()).estado()).isEqualTo((short) 1);
        assertThat(personaService.get(created.codper()).estado()).isEqualTo((short) 1);

        personaService.delete(created.codper());
        assertThat(personaRepository.findById(created.codper())).isEmpty();
        assertThatThrownBy(() -> personaService.get(created.codper())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enforcesUniqueCiThroughRealService() {
        personaService.create(createRequest("TEST-CRUD-DUP-001", "1"));

        assertThatThrownBy(() -> personaService.create(createRequest("TEST-CRUD-DUP-001", "1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void filtersInPostgreSqlBySearchTypeAndStatusWithPaginationAndSort() {
        PersonaResponse ana = personaService.create(new CreatePersonaRequest("TEST-FILTER-ANA", "Ana", "Ortega", "Mora",
                "F", "1", "ana.filter@example.test", "70000001", "A", null));
        PersonaResponse bruno = personaService.create(new CreatePersonaRequest("TEST-FILTER-BRU", "Bruno", "Pérez", "Ortega",
                "M", "0", "bruno.filter@example.test", "70000002", "I", null));

        var byName = personaService.list(PersonaSearchCriteria.from("ANA", null, null),
                org.springframework.data.domain.PageRequest.of(0, 10), null);
        assertThat(byName.content()).extracting(PersonaResponse::codper).contains(ana.codper());
        assertThat(personaService.list(PersonaSearchCriteria.from("orteg", "A", "1"),
                org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("ap")), null)
                .content()).extracting(PersonaResponse::codper).contains(ana.codper());
        assertThat(personaService.list(PersonaSearchCriteria.from("FILTER-BRU", "I", "0"),
                org.springframework.data.domain.PageRequest.of(0, 1), null).content())
                .extracting(PersonaResponse::codper).containsExactly(bruno.codper());
    }

    @Test
    void keepsOnlyApprovedTablesAndFlywayVersions() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                String.class);

        assertThat(tables).containsExactly("contrato_archivos", "contratos", "cuentas_pago", "cuotas", "flyway_schema_history", "menus", "mepro", "notificaciones", "otp_challenges", "pago_comprobantes", "pagos", "personas", "procesos", "propiedades", "recibos", "roles", "rolme", "rolusu", "sesiones_usuario", "unidad_fotos", "unidades", "usuarios");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15') AND success = true", Integer.class))
                .isEqualTo(15);
    }

    private CreatePersonaRequest createRequest(String ci, String estado) {
        return new CreatePersonaRequest(ci, "Nombre ficticio", "Paterno", "Materno", "F", estado,
                "persona@example.test", "70000000", "A", "foto");
    }

    private UpdatePersonaRequest updateRequest(String ci, String estado) {
        return new UpdatePersonaRequest(ci, "Nombre actualizado", " ", "Materno actualizado", "M", estado,
                "actualizada@example.test", "70000001", "I", "foto-actualizada");
    }
}
