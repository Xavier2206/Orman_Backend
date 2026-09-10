package com.orman.backend.payment.mapper;

import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.response.CuentaPagoResponse;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.person.entity.Persona;
import org.springframework.stereotype.Component;

@Component
public class CuentaPagoMapper {

    public CuentaPagoEntity toEntity(CuentaPagoRequest request, Persona propietaria) {
        CuentaPagoEntity cuenta = new CuentaPagoEntity();
        cuenta.setPropietaria(propietaria);
        update(cuenta, request);
        return cuenta;
    }

    public void update(CuentaPagoEntity cuenta, CuentaPagoRequest request) {
        cuenta.setBanco(request.banco().trim());
        cuenta.setNumeroCuenta(request.numeroCuenta().trim());
        cuenta.setTitular(request.titular().trim());
        cuenta.setQrUrl(trimToNull(request.qrUrl()));
        cuenta.setInstrucciones(trimToNull(request.instrucciones()));
        cuenta.setOrden(request.orden());
        cuenta.setEstado(Short.valueOf(request.estado()));
    }

    public CuentaPagoResponse toResponse(CuentaPagoEntity cuenta) {
        return new CuentaPagoResponse(cuenta.getCodcta(), cuenta.getPropietaria().getCodper(), cuenta.getBanco(),
                cuenta.getNumeroCuenta(), cuenta.getTitular(), cuenta.getQrUrl(), cuenta.getInstrucciones(),
                cuenta.getOrden(), cuenta.getEstado());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
