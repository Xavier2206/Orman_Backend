package com.orman.backend.property.entity;

import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadFotoRepository;
import com.orman.backend.property.repository.UnidadRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class PropertyPersistenceIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private UnidadFotoRepository unidadFotoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesTheThreeApprovedTablesWithVersionTen() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);

        assertThat(tables).contains("propiedades", "unidades", "unidad_fotos");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '10' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'propiedades'::regclass", String.class))
                .contains("pk_propiedades", "fk_propiedades_personas_propietaria", "ck_propiedades_tipo",
                        "ck_propiedades_coordenadas", "ck_propiedades_inversion_inicial", "ck_propiedades_estado");
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'unidad_fotos'",
                String.class)).contains("uk_unidad_fotos_coduni_portada", "uk_unidad_fotos_coduni_orden");
    }

    @Test
    void persistsTheApprovedRelationships() {
        Persona propietaria = personaRepository.saveAndFlush(persona("PROPERTY-OWNER-001"));
        PropiedadEntity propiedad = propiedadRepository.saveAndFlush(propiedad(propietaria));
        UnidadEntity unidad = unidadRepository.saveAndFlush(unidad(propiedad));
        UnidadFotoEntity foto = unidadFotoRepository.saveAndFlush(foto(unidad, 0, true));
        entityManager.clear();

        UnidadFotoEntity persisted = unidadFotoRepository.findById(foto.getId()).orElseThrow();
        assertThat(persisted.getUnidad().getCoduni()).isEqualTo(unidad.getCoduni());
        assertThat(persisted.getUnidad().getPropiedad().getPropietaria().getCodper()).isEqualTo(propietaria.getCodper());

    }

    @Test
    void rejectsASecondCoverForTheSameUnit() {
        UnidadEntity unidad = unidadRepository.saveAndFlush(unidad(
                propiedadRepository.saveAndFlush(propiedad(personaRepository.saveAndFlush(persona("PROPERTY-OWNER-002"))))));
        unidadFotoRepository.saveAndFlush(foto(unidad, 0, true));

        assertThatThrownBy(() -> unidadFotoRepository.saveAndFlush(foto(unidad, 1, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsARepeatedPhotoOrderForTheSameUnit() {
        UnidadEntity unidad = unidadRepository.saveAndFlush(unidad(
                propiedadRepository.saveAndFlush(propiedad(personaRepository.saveAndFlush(persona("PROPERTY-OWNER-003"))))));
        unidadFotoRepository.saveAndFlush(foto(unidad, 0, false));

        assertThatThrownBy(() -> unidadFotoRepository.saveAndFlush(foto(unidad, 0, false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidPropertyChecks() {
        Persona propietaria = personaRepository.saveAndFlush(persona("PROPERTY-OWNER-004"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO propiedades (nombre, tipo, direccion, ciudad, codper_propietaria, inversion_inicial, estado)
                VALUES ('Inválida', 'LOCAL', 'Dirección', 'La Paz', ?, -1, 1)
                """, propietaria.getCodper())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Propietaria de prueba");
        persona.setGenero('F');
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return persona;
    }

    private PropiedadEntity propiedad(Persona propietaria) {
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Casa de prueba");
        propiedad.setTipo("CASA");
        propiedad.setDireccion("Calle de prueba");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(new BigDecimal("1000.00"));
        propiedad.setEstado((short) 1);
        return propiedad;
    }

    private UnidadEntity unidad(PropiedadEntity propiedad) {
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Unidad 1");
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("50.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(new BigDecimal("2500.00"));
        unidad.setEstadoOperativo((short) 1);
        return unidad;
    }

    private UnidadFotoEntity foto(UnidadEntity unidad, int orden, boolean portada) {
        UnidadFotoEntity foto = new UnidadFotoEntity();
        foto.setUnidad(unidad);
        foto.setUrl("https://example.test/foto-" + orden + ".jpg");
        foto.setOrden(orden);
        foto.setPortada(portada);
        return foto;
    }
}
