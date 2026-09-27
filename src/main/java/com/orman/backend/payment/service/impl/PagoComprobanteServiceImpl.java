package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.mapper.PagoComprobanteMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.service.PagoComprobanteService;
import com.orman.backend.payment.service.PaymentImageContent;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagoComprobanteServiceImpl implements PagoComprobanteService {

    private final PagoComprobanteRepository comprobanteRepository;
    private final PagoComprobanteMapper comprobanteMapper;
    private final PaymentOwnershipService ownershipService;
    private final PaymentImageStorageService imageStorageService;

    @Override
    @Transactional(readOnly = true)
    public PagoComprobanteResponse getMetadata(Integer codpag, Authentication authentication) {
        PagoEntity pago = ownershipService.findAccessiblePago(codpag, authentication);
        PagoComprobanteEntity comprobante = findComprobante(pago.getCodpag());
        return comprobanteMapper.toResponse(comprobante);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentImageContent getImage(Integer codpag, Authentication authentication) {
        PagoEntity pago = ownershipService.findAccessiblePago(codpag, authentication);
        PagoComprobanteEntity comprobante = findComprobante(pago.getCodpag());
        return imageStorageService.loadProof(comprobante.getRutaArchivo(), pago.getCodpag());
    }

    private PagoComprobanteEntity findComprobante(Integer codpag) {
        return comprobanteRepository.findByPagoCodpag(codpag)
                .orElseThrow(() -> new ResourceNotFoundException("El comprobante de pago no existe."));
    }
}
