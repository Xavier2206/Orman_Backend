package com.orman.backend.contract.entity;

import com.orman.backend.contract.repository.ContratoArchivoRepository;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class ContratoPersistenceIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private ContratoArchivoRepository archivoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesTheContractTablesWithVersionEleven() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);

        assertThat(tables).contains("contratos", "contrato_archivos", "cuotas");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '11' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'contratos'::regclass", String.class))
                .contains("pk_contratos", "fk_contratos_unidades", "fk_contratos_personas_inquilino",
                        "ck_contratos_fechas", "ck_contratos_estado");
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'cuotas'", String.class))
                .contains("uk_cuotas_codcon_periodo");
    }

    @Test
    void persistsContractFilesAndPendingQuotas() {
        UnidadEntity unidad = unidad();
        Persona inquilino = personaRepository.saveAndFlush(persona("CT-TENANT-001"));
        ContratoEntity contrato = contratoRepository.saveAndFlush(contrato(unidad, inquilino, ContratoEstado.VIGENTE));
        ContratoArchivoEntity archivo = new ContratoArchivoEntity();
        archivo.setContrato(contrato);
        archivo.setUrl("https://example.test/contrato.pdf");
        archivo.setNombreArchivo("contrato.pdf");
        archivo.setOrden(0);
        archivoRepository.saveAndFlush(archivo);
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(contrato);
        cuota.setPeriodo(LocalDate.of(2026, 9, 1));
        cuota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        cuota.setMonto(new BigDecimal("2500.00"));
        cuota.setEstado(CuotaEstado.PENDIENTE);
        cuotaRepository.saveAndFlush(cuota);
        entityManager.clear();

        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(contrato.getCodcon()))
                .extracting(CuotaEntity::getEstado).containsExactly(CuotaEstado.PENDIENTE);
        assertThat(archivoRepository.findAllByContratoCodconOrderByOrdenAscIdAsc(contrato.getCodcon()))
                .extracting(ContratoArchivoEntity::getNombreArchivo).containsExactly("contrato.pdf");
    }

    @Test
    void rejectsAnotherCurrentContractForTheSameUnit() {
        UnidadEntity unidad = unidad();
        Persona one = personaRepository.saveAndFlush(persona("CT-TENANT-002"));
        Persona two = personaRepository.saveAndFlush(persona("CT-TENANT-003"));
        contratoRepository.saveAndFlush(contrato(unidad, one, ContratoEstado.VIGENTE));

        assertThatThrownBy(() -> contratoRepository.saveAndFlush(contrato(unidad, two, ContratoEstado.VIGENTE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidMonthlyDates() {
        UnidadEntity unidad = unidad();
        Persona inquilino = personaRepository.saveAndFlush(persona("CT-TENANT-004"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO contratos (coduni, codper_inquilino, fecha_inicio, fecha_fin, monto_mensual, garantia, estado)
                VALUES (?, ?, DATE '2026-09-02', DATE '2027-09-01', 2500, 0, 'BORRADOR')
                """, unidad.getCoduni(), inquilino.getCodper())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UnidadEntity unidad() {
        Persona propietaria = personaRepository.saveAndFlush(persona("CT-OWNER-" + System.nanoTime()));
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Casa contractual");
        propiedad.setTipo("CASA");
        propiedad.setDireccion("Calle contractual");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(new BigDecimal("1000.00"));
        propiedad.setEstado((short) 1);
        propiedad = propiedadRepository.saveAndFlush(propiedad);
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Unidad contractual");
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("50.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(new BigDecimal("2500.00"));
        unidad.setEstadoOperativo((short) 1);
        return unidadRepository.saveAndFlush(unidad);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci.substring(0, Math.min(ci.length(), 20)));
        persona.setNombre("Persona contractual");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return persona;
    }

    private ContratoEntity contrato(UnidadEntity unidad, Persona inquilino, ContratoEstado estado) {
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(inquilino);
        contrato.setFechaInicio(LocalDate.of(2026, 9, 1));
        contrato.setFechaFin(LocalDate.of(2027, 9, 1));
        contrato.setMontoMensual(new BigDecimal("2500.00"));
        contrato.setGarantia(BigDecimal.ZERO);
        contrato.setEstado(estado);
        if (estado != ContratoEstado.BORRADOR) {
            contrato.setFechaConfirmacion(LocalDateTime.now());
        }
        return contrato;
    }
}
