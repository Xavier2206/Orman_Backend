package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.payment.dto.response.ReciboResponse;
import com.orman.backend.payment.entity.ReciboEntity;
import com.orman.backend.payment.mapper.ReciboMapper;
import com.orman.backend.payment.repository.ReciboRepository;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.payment.service.ReciboService;
import com.orman.backend.property.service.PropertyOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReciboServiceImpl implements ReciboService {

    private final ReciboRepository reciboRepository;
    private final ReciboMapper reciboMapper;
    private final PaymentOwnershipService paymentOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    public ReciboResponse getByPago(Integer codpag, Authentication authentication) {
        paymentOwnershipService.findOwnedPago(codpag, authentication);
        return reciboMapper.toResponse(reciboRepository.findByPagoCodpag(codpag)
                .orElseThrow(() -> new ResourceNotFoundException("El Recibo para el Pago solicitado no existe.")));
    }

    @Override
    public ReciboResponse get(Integer codrec, Authentication authentication) {
        ReciboEntity recibo = reciboRepository.findByCodrec(codrec)
                .orElseThrow(() -> new ResourceNotFoundException("El Recibo solicitado no existe."));
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                recibo.getPago().getCuota().getContrato().getUnidad().getPropiedad().getPropietaria());
        return reciboMapper.toResponse(recibo);
    }
}
