package com.orman.backend.property.integration;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.PropiedadResumenProjection;
import com.orman.backend.property.repository.UnidadRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class PropiedadResumenIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;

    @Test
    void aggregatesOnlyTheRequestedOwnerAndUsesOperationalUnitsForOccupancy() {
        Persona owner = persona("SUMMARY-OWNER-001");
        Persona otherOwner = persona("SUMMARY-OWNER-002");
        Persona tenant = persona("SUMMARY-TENANT-001");

        PropiedadEntity casaActiva = propiedad(owner, "Casa resumen", "CASA", new BigDecimal("1000.00"), (short) 1);
        PropiedadEntity edificioActivo = propiedad(owner, "Edificio resumen", "EDIFICIO", new BigDecimal("2000.00"), (short) 1);
        PropiedadEntity casaInactiva = propiedad(owner, "Casa inactiva", "CASA", new BigDecimal("500.00"), (short) 0);
        propiedad(otherOwner, "Propiedad ajena", "EDIFICIO", new BigDecimal("9000.00"), (short) 1);

        UnidadEntity casaOcupada = unidad(casaActiva, "Casa ocupada", (short) 1);
        UnidadEntity casaDisponible = unidad(casaActiva, "Casa disponible", (short) 1);
        UnidadEntity casaNoHabilitada = unidad(casaActiva, "Casa no habilitada", (short) 0);
        UnidadEntity edificioDisponible = unidad(edificioActivo, "Edificio disponible", (short) 1);
        UnidadEntity inactivaOcupada = unidad(casaInactiva, "Inactiva ocupada", (short) 1);
        UnidadEntity finalizada = unidad(edificioActivo, "Contrato finalizado", (short) 1);
        UnidadEntity rescindida = unidad(edificioActivo, "Contrato rescindido", (short) 1);

        contrato(casaOcupada, tenant, ContratoEstado.VIGENTE);
        contrato(casaNoHabilitada, tenant, ContratoEstado.VIGENTE);
        contrato(edificioDisponible, tenant, ContratoEstado.BORRADOR);
        contrato(inactivaOcupada, tenant, ContratoEstado.VIGENTE);
        contrato(finalizada, tenant, ContratoEstado.FINALIZADO);
        contrato(rescindida, tenant, ContratoEstado.RESCINDIDO);

        PropiedadResumenProjection summary = propiedadRepository.findResumenByPropietaria(owner.getCodper());

        assertThat(summary.getInversionTotal()).isEqualByComparingTo("3500.00");
        assertThat(summary.getPropiedadesActivas()).isEqualTo(2);
        assertThat(summary.getCasasActivas()).isEqualTo(1);
        assertThat(summary.getEdificiosActivos()).isEqualTo(1);
        assertThat(summary.getUnidadesTotales()).isEqualTo(7);
        assertThat(summary.getUnidadesHabilitadas()).isEqualTo(6);
        assertThat(summary.getUnidadesNoHabilitadas()).isEqualTo(1);
        assertThat(summary.getUnidadesOcupadas()).isEqualTo(2);
    }

    @Test
    void returnsZeroForAllCountsAndOccupancyWhenOwnerHasNoProperties() {
        Persona owner = persona("SUMMARY-OWNER-003");

        PropiedadResumenProjection summary = propiedadRepository.findResumenByPropietaria(owner.getCodper());

        assertThat(summary.getInversionTotal()).isEqualByComparingTo("0.00");
        assertThat(summary.getPropiedadesActivas()).isZero();
        assertThat(summary.getCasasActivas()).isZero();
        assertThat(summary.getEdificiosActivos()).isZero();
        assertThat(summary.getUnidadesTotales()).isZero();
        assertThat(summary.getUnidadesHabilitadas()).isZero();
        assertThat(summary.getUnidadesNoHabilitadas()).isZero();
        assertThat(summary.getUnidadesOcupadas()).isZero();
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona resumen");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private PropiedadEntity propiedad(Persona owner, String nombre, String tipo, BigDecimal inversion, Short estado) {
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre(nombre);
        propiedad.setTipo(tipo);
        propiedad.setDireccion("Calle resumen");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(owner);
        propiedad.setInversionInicial(inversion);
        propiedad.setEstado(estado);
        return propiedadRepository.saveAndFlush(propiedad);
    }

    private UnidadEntity unidad(PropiedadEntity propiedad, String nombre, Short estadoOperativo) {
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre(nombre);
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("50.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(new BigDecimal("2500.00"));
        unidad.setEstadoOperativo(estadoOperativo);
        return unidadRepository.saveAndFlush(unidad);
    }

    private void contrato(UnidadEntity unidad, Persona tenant, ContratoEstado estado) {
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(tenant);
        contrato.setFechaInicio(LocalDate.of(2026, 9, 1));
        contrato.setFechaFin(LocalDate.of(2027, 9, 1));
        contrato.setMontoMensual(new BigDecimal("2500.00"));
        contrato.setGarantia(BigDecimal.ZERO);
        contrato.setEstado(estado);
        if (estado != ContratoEstado.BORRADOR) {
            contrato.setFechaConfirmacion(LocalDateTime.of(2026, 8, 1, 12, 0));
        }
        if (estado == ContratoEstado.RESCINDIDO) {
            contrato.setFechaRescision(LocalDate.of(2026, 9, 15));
            contrato.setMotivoRescision("Prueba de rescisión");
        }
        contratoRepository.saveAndFlush(contrato);
    }
}
