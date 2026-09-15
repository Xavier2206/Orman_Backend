package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.UnidadEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
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

        CuotaEntity cuota = cuotaMapper.toPendingEntity(contrato, LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(cuota, "codcuo", 30);
        assertThat(cuota.getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThat(cuotaMapper.toResponse(cuota, BigDecimal.ZERO, BigDecimal.ZERO).fechaVencimiento())
                .isEqualTo(LocalDate.of(2026, 9, 1));

        var archivo = archivoMapper.toEntity(new ContratoArchivoRequest("https://example.test/contrato.pdf",
                " Contrato firmado.pdf ", " ", 0), contrato);
        ReflectionTestUtils.setField(archivo, "id", 8);
        assertThat(archivo.getTipoContenido()).isNull();
        assertThat(archivoMapper.toResponse(archivo).nombreArchivo()).isEqualTo("Contrato firmado.pdf");
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
