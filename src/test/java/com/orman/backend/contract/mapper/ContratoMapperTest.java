package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.UnidadEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ContratoMapperTest {

    private final ContratoMapper contratoMapper = new ContratoMapper();
    private final ContratoArchivoMapper archivoMapper = new ContratoArchivoMapper();
    private final CuotaMapper cuotaMapper = new CuotaMapper();

    @Test
    void mapsRegisteredContractFileAndPendingQuotaWithoutExposingEntities() {
        UnidadEntity unidad = unidad(9);
        Persona inquilino = persona(4);
        ContratoEntity contrato = contratoMapper.toEntity(request(), unidad, inquilino, ContratoEstado.VIGENTE,
                java.time.LocalDateTime.of(2026, 9, 1, 12, 0));
        ReflectionTestUtils.setField(contrato, "codcon", 12);

        assertThat(contrato.getEstado()).isEqualTo(ContratoEstado.VIGENTE);
        assertThat(contrato.getMoneda()).isEqualTo("BOB");
        assertThat(contratoMapper.toResponse(contrato).coduni()).isEqualTo(9);
        assertThat(contratoMapper.toResponse(contrato).fechaRegistro()).isEqualTo(OffsetDateTime.of(
                2026, 9, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)));

        CuotaEntity cuota = cuotaMapper.toPendingEntity(contrato, LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(cuota, "codcuo", 30);
        assertThat(cuota.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThat(cuotaMapper.toResponse(cuota, BigDecimal.ZERO, BigDecimal.ZERO).fechaVencimiento())
                .isEqualTo(LocalDate.of(2026, 9, 1));

        var uploadedAt = java.time.LocalDateTime.of(2026, 9, 16, 12, 0);
        var archivo = archivoMapper.toEntity(contrato, "Contrato firmado.pdf", "uuid.pdf",
                "contratos/12/uuid.pdf", 1200, 900, uploadedAt, "propietaria", 0);
        ReflectionTestUtils.setField(archivo, "id", 8);
        assertThat(archivo.getTipoContenido()).isEqualTo("application/pdf");
        assertThat(archivo.getTamanoOriginal()).isEqualTo(1200L);
        assertThat(archivo.getTamanoFinal()).isEqualTo(900L);
        assertThat(archivo.getFechaSubida()).isEqualTo(uploadedAt);
        assertThat(archivo.getUrl()).isNull();
        assertThat(archivoMapper.toResponse(archivo).nombreArchivo()).isEqualTo("Contrato firmado.pdf");
        assertThat(archivoMapper.toResponse(archivo).almacenadoInternamente()).isTrue();
    }

    @Test
    void mapsEnrichedContractResponseWithInquilinoUnidadPropiedadAndCuotas() {
        com.orman.backend.property.entity.PropiedadEntity propiedad = new com.orman.backend.property.entity.PropiedadEntity();
        ReflectionTestUtils.setField(propiedad, "codprop", 3);
        propiedad.setNombre("Edificio Central");

        UnidadEntity unidad = unidad(9);
        unidad.setNombre("Dpto. 2A");
        unidad.setTipoUnidad("Residencial");
        unidad.setDescripcion("Departamento amoblado");
        unidad.setPiso(2);
        unidad.setPropiedad(propiedad);

        Persona inquilino = persona(4);
        inquilino.setNombre("Carlos Eduardo");
        inquilino.setAp("Mendoza");
        inquilino.setAm("Rojas");
        inquilino.setCi("4892014 SC");

        ContratoEntity contrato = contratoMapper.toEntity(request(), unidad, inquilino, ContratoEstado.VIGENTE,
                java.time.LocalDateTime.of(2026, 9, 1, 12, 0));
        ReflectionTestUtils.setField(contrato, "codcon", 12);

        com.orman.backend.contract.dto.response.ContratoCuotasResumenResponse cuotas =
                new com.orman.backend.contract.dto.response.ContratoCuotasResumenResponse(12, 8, 4, new BigDecimal("6000.00"));
        com.orman.backend.contract.dto.response.ContratoResponse response = contratoMapper.toResponse(contrato, cuotas);

        assertThat(response.codcon()).isEqualTo(12);
        assertThat(response.inquilino()).isNotNull();
        assertThat(response.inquilino().codper()).isEqualTo(4);
        assertThat(response.inquilino().nombreCompleto()).isEqualTo("Carlos Eduardo Mendoza Rojas");
        assertThat(response.inquilino().ci()).isEqualTo("4892014 SC");

        assertThat(response.unidad()).isNotNull();
        assertThat(response.unidad().coduni()).isEqualTo(9);
        assertThat(response.unidad().nombre()).isEqualTo("Dpto. 2A");
        assertThat(response.unidad().tipoUnidad()).isEqualTo("Residencial");
        assertThat(response.unidad().descripcion()).isEqualTo("Departamento amoblado");
        assertThat(response.unidad().piso()).isEqualTo(2);

        assertThat(response.propiedad()).isNotNull();
        assertThat(response.propiedad().codprop()).isEqualTo(3);
        assertThat(response.propiedad().nombre()).isEqualTo("Edificio Central");

        assertThat(response.cuotas()).isNotNull();
        assertThat(response.cuotas().totalCuotas()).isEqualTo(12);
        assertThat(response.cuotas().cuotasPagadas()).isEqualTo(8);
        assertThat(response.cuotas().cuotasPendientes()).isEqualTo(4);
        assertThat(response.cuotas().saldoPendiente()).isEqualByComparingTo("6000.00");
    }

    @Test
    void persistsNewContractRegistrationUsingBoliviaWallTime() {
        LocalDateTime registeredAt = OrmanTimeConfig.businessNow(
                Clock.fixed(Instant.parse("2026-09-30T16:20:00Z"), ZoneOffset.UTC));

        ContratoEntity contrato = contratoMapper.toEntity(request(), unidad(9), persona(4),
                ContratoEstado.VIGENTE, registeredAt);

        assertThat(contrato.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 20));
        assertThat(contratoMapper.toResponse(contrato).fechaRegistro()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 20, 0, 0, ZoneOffset.ofHours(-4)));
    }

    private ContratoRequest request() {
        return new ContratoRequest(4, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1),
                new BigDecimal("2500.00"), new BigDecimal("2500.00"));
    }

    private UnidadEntity unidad(Integer coduni) {
        UnidadEntity unidad = new UnidadEntity();
        ReflectionTestUtils.setField(unidad, "coduni", coduni);
        return unidad;
    }

    private Persona persona(Integer codper) {
        Persona persona = new Persona();
        ReflectionTestUtils.setField(persona, "codper", codper);
        return persona;
    }
}
