package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.UnidadEntity;
import org.springframework.stereotype.Component;

@Component
public class ContratoMapper {

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
        return new ContratoResponse(contrato.getCodcon(), contrato.getUnidad().getCoduni(),
                contrato.getInquilino().getCodper(), contrato.getFechaInicio(), contrato.getFechaFin(),
                contrato.getMontoMensual(), contrato.getMoneda(), contrato.getGarantia(), contrato.getEstado().name(),
                contrato.getFechaRegistro(), contrato.getFechaRescision(), contrato.getMotivoRescision());
    }

    private void apply(ContratoEntity contrato, ContratoRequest request) {
        contrato.setFechaInicio(request.fechaInicio());
        contrato.setFechaFin(request.fechaFin());
        contrato.setMontoMensual(request.montoMensual());
        contrato.setGarantia(request.garantia());
    }
}
