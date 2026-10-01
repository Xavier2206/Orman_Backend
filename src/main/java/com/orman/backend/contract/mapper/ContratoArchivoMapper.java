package com.orman.backend.contract.mapper;

import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.dto.response.ContratoArchivoResponse;
import com.orman.backend.contract.entity.ContratoArchivoEntity;
import com.orman.backend.contract.entity.ContratoEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ContratoArchivoMapper {

    public ContratoArchivoEntity toEntity(ContratoEntity contrato, String nombreOriginal, String nombreAlmacenado,
                                          String rutaRef, long tamanoOriginal, long tamanoFinal,
                                          LocalDateTime fechaSubida, String subidoPor, Integer orden) {
        ContratoArchivoEntity archivo = new ContratoArchivoEntity();
        archivo.setContrato(contrato);
        archivo.setNombreArchivo(nombreOriginal);
        archivo.setNombreAlmacenado(nombreAlmacenado);
        archivo.setRutaRef(rutaRef);
        archivo.setTipoContenido("application/pdf");
        archivo.setTamanoOriginal(tamanoOriginal);
        archivo.setTamanoFinal(tamanoFinal);
        archivo.setFechaSubida(fechaSubida);
        archivo.setSubidoPor(subidoPor);
        archivo.setOrden(orden);
        return archivo;
    }

    public ContratoArchivoResponse toResponse(ContratoArchivoEntity archivo) {
        return new ContratoArchivoResponse(archivo.getId(), archivo.getContrato().getCodcon(),
                archivo.getNombreArchivo(), archivo.getTipoContenido(), archivo.getTamanoOriginal(),
                archivo.getTamanoFinal(), OrmanTimeConfig.ormanLocalToOffset(archivo.getFechaSubida()),
                archivo.getSubidoPor(), archivo.getOrden(),
                archivo.getRutaRef() != null);
    }
}
