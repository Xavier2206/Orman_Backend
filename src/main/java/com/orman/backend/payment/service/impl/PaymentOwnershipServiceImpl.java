package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.CuentaPagoRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.property.service.PropertyOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentOwnershipServiceImpl implements PaymentOwnershipService {

    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final CuentaPagoRepository cuentaPagoRepository;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    public CuotaEntity findOwnedCuota(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = cuotaRepository.findByCodcuo(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
        assertOwner(authentication, cuota);
        return cuota;
    }

    @Override
    public CuotaEntity findCuota(Integer codcuo) {
        return cuotaRepository.findByCodcuo(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
    }

    @Override
    @Transactional
    public CuotaEntity findOwnedCuotaForUpdate(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = cuotaRepository.findByCodcuoForUpdate(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
        assertOwner(authentication, cuota);
        return cuota;
    }

    @Override
    public PagoEntity findOwnedPago(Integer codpag, Authentication authentication) {
        PagoEntity pago = pagoRepository.findByCodpag(codpag).orElseThrow(() -> pagoNotFound(codpag));
        assertOwner(authentication, pago.getCuota());
        return pago;
    }

    @Override
    @Transactional
    public PagoEntity findOwnedPagoForUpdate(Integer codpag, Authentication authentication) {
        PagoEntity pago = pagoRepository.findByCodpagForUpdate(codpag).orElseThrow(() -> pagoNotFound(codpag));
        assertOwner(authentication, pago.getCuota());
        return pago;
    }

    @Override
    public CuentaPagoEntity findOwnedCuentaPago(Integer codcta, Authentication authentication) {
        CuentaPagoEntity cuenta = findCuentaPago(codcta);
        propertyOwnershipService.assertCurrentPropietaria(authentication, cuenta.getPropietaria());
        return cuenta;
    }

    @Override
    public CuentaPagoEntity findCuentaPago(Integer codcta) {
        return cuentaPagoRepository.findByCodcta(codcta)
                .orElseThrow(() -> new ResourceNotFoundException("La CuentaPago solicitada no existe."));
    }

    private void assertOwner(Authentication authentication, CuotaEntity cuota) {
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                cuota.getContrato().getUnidad().getPropiedad().getPropietaria());
    }

    private ResourceNotFoundException cuotaNotFound(Integer codcuo) {
        return new ResourceNotFoundException("La Cuota solicitada no existe: " + codcuo);
    }

    private ResourceNotFoundException pagoNotFound(Integer codpag) {
        return new ResourceNotFoundException("El Pago solicitado no existe: " + codpag);
    }
}
