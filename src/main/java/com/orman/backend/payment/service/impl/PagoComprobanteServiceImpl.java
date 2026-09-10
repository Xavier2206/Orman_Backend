package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.response.PagoComprobanteResponse;
import com.orman.backend.payment.event.ComprobanteRecibidoEvent;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.mapper.PagoComprobanteMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.service.PagoComprobanteService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagoComprobanteServiceImpl implements PagoComprobanteService {

    private final PagoComprobanteRepository pagoComprobanteRepository;
    private final PagoComprobanteMapper pagoComprobanteMapper;
    private final PaymentOwnershipService paymentOwnershipService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public PagoComprobanteResponse create(Integer codpag, PagoComprobanteRequest request, Authentication authentication) {
        PagoEntity pago = paymentOwnershipService.findOwnedPago(codpag, authentication);
        if (pago.getEstado() != PagoEstado.PENDIENTE_REVISION) {
            throw new BusinessRuleException("Solo se pueden agregar comprobantes a Pagos pendientes de revisión.");
        }
        if (pagoComprobanteRepository.existsByPagoCodpagAndOrden(codpag, request.orden())) {
            throw duplicateOrden();
        }
        try {
            PagoComprobanteResponse response = pagoComprobanteMapper.toResponse(pagoComprobanteRepository.saveAndFlush(
                    pagoComprobanteMapper.toEntity(request, pago)));
            applicationEventPublisher.publishEvent(new ComprobanteRecibidoEvent(pago.getCodpag()));
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoComprobanteResponse> listByPago(Integer codpag, Authentication authentication) {
        paymentOwnershipService.findOwnedPago(codpag, authentication);
        return pagoComprobanteRepository.findAllByPagoCodpagOrderByOrdenAscIdAsc(codpag).stream()
                .map(pagoComprobanteMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Integer codpag, Integer id, Authentication authentication) {
        paymentOwnershipService.findOwnedPago(codpag, authentication);
        PagoComprobanteEntity comprobante = pagoComprobanteRepository.findByIdAndPagoCodpag(id, codpag)
                .orElseThrow(() -> new ResourceNotFoundException("El comprobante solicitado no existe."));
        if (comprobante.getPago().getEstado() != PagoEstado.PENDIENTE_REVISION) {
            throw new BusinessRuleException("Solo se pueden eliminar comprobantes de Pagos pendientes de revisión.");
        }
        pagoComprobanteRepository.delete(comprobante);
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden del comprobante ya existe en este Pago.");
    }
}
