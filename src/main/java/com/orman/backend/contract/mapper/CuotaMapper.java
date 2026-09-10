package com.orman.backend.contract.mapper;

import com.orman.backend.contract.dto.response.CuotaResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class CuotaMapper {

    public CuotaEntity toPendingEntity(ContratoEntity contrato, LocalDate periodo) {
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(contrato);
        cuota.setPeriodo(periodo);
        cuota.setFechaVencimiento(periodo);
        cuota.setMonto(contrato.getMontoMensual());
        cuota.setEstado(CuotaEstado.PENDIENTE);
        return cuota;
    }

    public CuotaResponse toResponse(CuotaEntity cuota) {
        return new CuotaResponse(cuota.getCodcuo(), cuota.getContrato().getCodcon(), cuota.getPeriodo(),
                cuota.getFechaVencimiento(), cuota.getMonto(), cuota.getEstado().name());
    }
}
