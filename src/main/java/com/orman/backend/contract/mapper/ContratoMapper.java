package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.request.ContratoRenovacionRequest;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.entity.UnidadEntity;
import org.springframework.stereotype.Component;

@Component
public class ContratoMapper {

    public ContratoEntity toEntity(ContratoRequest request, UnidadEntity unidad, Persona inquilino) {
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(inquilino);
        contrato.setEstado(ContratoEstado.BORRADOR);
        apply(contrato, request);
        return contrato;
    }

    public ContratoEntity toRenewalEntity(ContratoRenovacionRequest request, ContratoEntity origen) {
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(origen.getUnidad());
        contrato.setInquilino(origen.getInquilino());
        contrato.setContratoOrigen(origen);
        contrato.setEstado(ContratoEstado.BORRADOR);
        contrato.setFechaInicio(request.fechaInicio());
        contrato.setFechaFin(request.fechaFin());
        contrato.setMontoMensual(request.montoMensual());
        contrato.setGarantia(request.garantia());
        return contrato;
    }

    public void update(ContratoEntity contrato, ContratoRequest request, Persona inquilino) {
        contrato.setInquilino(inquilino);
        apply(contrato, request);
    }

    public ContratoResponse toResponse(ContratoEntity contrato) {
        return new ContratoResponse(contrato.getCodcon(), contrato.getUnidad().getCoduni(),
                contrato.getInquilino().getCodper(), contrato.getFechaInicio(), contrato.getFechaFin(),
                contrato.getMontoMensual(), contrato.getGarantia(), contrato.getEstado().name(),
                contrato.getFechaConfirmacion(), contrato.getFechaRescision(), contrato.getMotivoRescision(),
                contrato.getContratoOrigen() == null ? null : contrato.getContratoOrigen().getCodcon());
    }

    private void apply(ContratoEntity contrato, ContratoRequest request) {
        contrato.setFechaInicio(request.fechaInicio());
        contrato.setFechaFin(request.fechaFin());
        contrato.setMontoMensual(request.montoMensual());
        contrato.setGarantia(request.garantia());
    }
}
