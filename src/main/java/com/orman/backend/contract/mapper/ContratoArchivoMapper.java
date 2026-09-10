package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.request.ContratoArchivoRequest;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.entity.ContratoArchivoEntity;
import com.orman.backend.contract.entity.ContratoEntity;
import org.springframework.stereotype.Component;

@Component
public class ContratoArchivoMapper {

    public ContratoArchivoEntity toEntity(ContratoArchivoRequest request, ContratoEntity contrato) {
        ContratoArchivoEntity archivo = new ContratoArchivoEntity();
        archivo.setContrato(contrato);
        archivo.setUrl(request.url().trim());
        archivo.setNombreArchivo(request.nombreArchivo().trim());
        archivo.setTipoContenido(blankToNull(request.tipoContenido()));
        archivo.setOrden(request.orden());
        return archivo;
    }

    public ContratoArchivoResponse toResponse(ContratoArchivoEntity archivo) {
        return new ContratoArchivoResponse(archivo.getId(), archivo.getContrato().getCodcon(), archivo.getUrl(),
                archivo.getNombreArchivo(), archivo.getTipoContenido(), archivo.getOrden());
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
