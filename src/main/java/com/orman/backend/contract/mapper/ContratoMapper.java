package com.orman.backend.contract.mapper;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.response.ContratoCuotasResumenResponse;
import com.orman.backend.contract.dto.response.ContratoInquilinoResponse;
import com.orman.backend.contract.dto.response.ContratoPropiedadResponse;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.dto.response.ContratoUnidadResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ContratoMapper {

    private static final ContratoCuotasResumenResponse DEFAULT_CUOTAS =
            new ContratoCuotasResumenResponse(0, 0, 0, BigDecimal.ZERO.setScale(2));

    public ContratoEntity toEntity(ContratoRequest request, UnidadEntity unidad, Persona inquilino,
                                   ContratoEstado estado, java.time.LocalDateTime fechaRegistro) {
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(inquilino);
        contrato.setEstado(estado);
        contrato.setFechaRegistro(fechaRegistro);
        contrato.setMoneda("BOB");
        apply(contrato, request);
        return contrato;
    }

    public ContratoResponse toResponse(ContratoEntity contrato) {
        return toResponse(contrato, null);
    }

    public ContratoResponse toResponse(ContratoEntity contrato, ContratoCuotasResumenResponse cuotas) {
        ContratoInquilinoResponse inquilino = toInquilinoResponse(contrato.getInquilino());
        ContratoUnidadResponse unidad = toUnidadResponse(contrato.getUnidad());
        ContratoPropiedadResponse propiedad = (contrato.getUnidad() != null && contrato.getUnidad().getPropiedad() != null)
                ? toPropiedadResponse(contrato.getUnidad().getPropiedad())
                : null;
        ContratoCuotasResumenResponse cuotasResumen = cuotas != null ? cuotas : DEFAULT_CUOTAS;

        return new ContratoResponse(contrato.getCodcon(),
                contrato.getUnidad() != null ? contrato.getUnidad().getCoduni() : null,
                contrato.getInquilino() != null ? contrato.getInquilino().getCodper() : null,
                contrato.getFechaInicio(), contrato.getFechaFin(),
                contrato.getMontoMensual(), contrato.getMoneda(), contrato.getGarantia(), contrato.getEstado().name(),
                OrmanTimeConfig.ormanLocalToOffset(contrato.getFechaRegistro()),
                contrato.getFechaRescision(), contrato.getMotivoRescision(),
                inquilino, unidad, propiedad, cuotasResumen);
    }

    public ContratoInquilinoResponse toInquilinoResponse(Persona inquilino) {
        if (inquilino == null) {
            return null;
        }
        return new ContratoInquilinoResponse(inquilino.getCodper(), formatNombreCompleto(inquilino), inquilino.getCi());
    }

    private ContratoUnidadResponse toUnidadResponse(UnidadEntity unidad) {
        if (unidad == null) {
            return null;
        }
        return new ContratoUnidadResponse(unidad.getCoduni(), unidad.getNombre(), unidad.getTipoUnidad(),
                unidad.getDescripcion(), unidad.getPiso());
    }

    private ContratoPropiedadResponse toPropiedadResponse(PropiedadEntity propiedad) {
        if (propiedad == null) {
            return null;
        }
        return new ContratoPropiedadResponse(propiedad.getCodprop(), propiedad.getNombre());
    }

    private String formatNombreCompleto(Persona persona) {
        if (persona == null) {
            return null;
        }
        String full = Stream.of(persona.getNombre(), persona.getAp(), persona.getAm())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        return full.isEmpty() ? null : full;
    }

    private void apply(ContratoEntity contrato, ContratoRequest request) {
        contrato.setFechaInicio(request.fechaInicio());
        contrato.setFechaFin(request.fechaFin());
        contrato.setMontoMensual(request.montoMensual());
        contrato.setGarantia(request.garantia());
    }
}
