package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.response.InquilinoContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import org.springframework.stereotype.Component;

@Component
public class InquilinoPortalMapper {

    public InquilinoContratoResponse toResponse(ContratoEntity contrato) {
        return new InquilinoContratoResponse(contrato.getCodcon(), contrato.getEstado().name(),
                contrato.getFechaInicio(), contrato.getFechaFin(), contrato.getFechaRescision(),
                contrato.getMotivoRescision(), contrato.getMontoMensual(), contrato.getMoneda(),
                contrato.getGarantia(), contrato.getUnidad().getPropiedad().getCodprop(),
                contrato.getUnidad().getPropiedad().getNombre(), contrato.getUnidad().getCoduni(),
                contrato.getUnidad().getNombre());
    }
}
