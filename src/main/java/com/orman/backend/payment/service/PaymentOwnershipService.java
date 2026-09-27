package com.orman.backend.payment.service;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.payment.entity.PagoEntity;
import org.springframework.security.core.Authentication;

public interface PaymentOwnershipService {

    CuotaEntity findOwnedCuota(Integer codcuo, Authentication authentication);

    CuotaEntity findCuota(Integer codcuo);

    CuotaEntity findAccessibleCuota(Integer codcuo, Authentication authentication);

    CuotaEntity findOwnedCuotaForUpdate(Integer codcuo, Authentication authentication);

    PagoEntity findOwnedPago(Integer codpag, Authentication authentication);

    PagoEntity findOwnedPagoForUpdate(Integer codpag, Authentication authentication);

    PagoEntity findAccessiblePago(Integer codpag, Authentication authentication);
}
