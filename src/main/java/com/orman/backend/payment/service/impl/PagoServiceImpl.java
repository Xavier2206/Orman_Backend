package com.orman.backend.payment.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.event.PagoConfirmadoEvent;
import com.orman.backend.payment.event.PagoRechazadoEvent;
import com.orman.backend.payment.entity.CuentaPagoEntity;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.mapper.PagoMapper;
import com.orman.backend.payment.mapper.ReciboMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.repository.ReciboRepository;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;
    private final PagoComprobanteRepository pagoComprobanteRepository;
    private final ReciboRepository reciboRepository;
    private final PagoMapper pagoMapper;
    private final ReciboMapper reciboMapper;
    private final PaymentOwnershipService paymentOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public PagoResponse create(Integer codcuo, PagoRequest request, Authentication authentication) {
        CuotaEntity cuota = paymentOwnershipService.findOwnedCuota(codcuo, authentication);
        validateCuotaCanReceivePayment(cuota);
        if (pagoRepository.findByIdempotencyKey(request.idempotencyKey()).isPresent()) {
            throw new ConflictException("La clave de idempotencia ya fue utilizada.");
        }
        CuentaPagoEntity cuentaPago = validateCuentaPago(request, authentication);
        validateAvailableAmount(cuota, request.monto());
        try {
            return pagoMapper.toResponse(pagoRepository.saveAndFlush(
                    pagoMapper.toPendingEntity(request, cuota, cuentaPago)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("No se pudo registrar el Pago por un conflicto de integridad.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listByCuota(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = paymentOwnershipService.findOwnedCuota(codcuo, authentication);
        Integer codper = cuota.getContrato().getUnidad().getPropiedad().getPropietaria().getCodper();
        return pagoRepository.findAllByCuotaOwned(codcuo, codper).stream().map(pagoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PagoResponse> list(PagoEstado estado, MetodoPago metodo, Pageable pageable,
                                           Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<PagoResponse> page = pagoRepository.searchOwned(codper, estado, metodo, pageable).map(pagoMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse get(Integer codpag, Authentication authentication) {
        return pagoMapper.toResponse(paymentOwnershipService.findOwnedPago(codpag, authentication));
    }

    @Override
    @Transactional
    public PagoResponse confirm(Integer codpag, Authentication authentication) {
        PagoEntity initialPago = paymentOwnershipService.findOwnedPago(codpag, authentication);
        CuotaEntity cuota = paymentOwnershipService.findOwnedCuotaForUpdate(initialPago.getCuota().getCodcuo(), authentication);
        PagoEntity pago = paymentOwnershipService.findOwnedPagoForUpdate(codpag, authentication);
        validatePending(pago, "confirmar");
        validateCuentaForConfirmation(pago);
        validateComprobanteForConfirmation(pago);

        BigDecimal confirmedBefore = pagoRepository.sumConfirmedMontoByCuota(cuota.getCodcuo());
        BigDecimal confirmedAfter = confirmedBefore.add(pago.getMonto());
        if (confirmedAfter.compareTo(cuota.getMonto()) > 0) {
            throw new BusinessRuleException("El Pago supera el saldo pendiente de la Cuota.");
        }

        pago.setEstado(PagoEstado.CONFIRMADO);
        pago.setFechaRevision(LocalDateTime.now());
        pago.setMotivoRechazo(null);
        pago.setMotivoAnulacion(null);
        cuota.setEstado(estadoCuota(cuota.getMonto(), confirmedAfter));
        cuotaRepository.save(cuota);
        if (reciboRepository.existsByPagoCodpag(codpag)) {
            throw new ConflictException("El Pago ya tiene un Recibo generado.");
        }
        reciboRepository.save(reciboMapper.toEntity(pago));
        applicationEventPublisher.publishEvent(new PagoConfirmadoEvent(pago.getCodpag()));
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional
    public PagoResponse reject(Integer codpag, PagoMotivoRequest request, Authentication authentication) {
        PagoEntity pago = paymentOwnershipService.findOwnedPagoForUpdate(codpag, authentication);
        validatePending(pago, "rechazar");
        pago.setEstado(PagoEstado.RECHAZADO);
        pago.setFechaRevision(LocalDateTime.now());
        pago.setMotivoRechazo(request.motivo().trim());
        pago.setMotivoAnulacion(null);
        applicationEventPublisher.publishEvent(new PagoRechazadoEvent(pago.getCodpag()));
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional
    public PagoResponse annul(Integer codpag, PagoMotivoRequest request, Authentication authentication) {
        PagoEntity pago = paymentOwnershipService.findOwnedPagoForUpdate(codpag, authentication);
        validatePending(pago, "anular");
        pago.setEstado(PagoEstado.ANULADO);
        pago.setFechaRevision(LocalDateTime.now());
        pago.setMotivoRechazo(null);
        pago.setMotivoAnulacion(request.motivo().trim());
        return pagoMapper.toResponse(pago);
    }

    private CuentaPagoEntity validateCuentaPago(PagoRequest request, Authentication authentication) {
        if (request.metodo() == MetodoPago.EFECTIVO) {
            if (request.codcta() != null) {
                throw new BusinessRuleException("Los Pagos en efectivo no deben indicar una CuentaPago.");
            }
            return null;
        }
        if (request.codcta() == null) {
            throw new BusinessRuleException("Transferencia y QR requieren una CuentaPago activa.");
        }
        CuentaPagoEntity cuentaPago = paymentOwnershipService.findOwnedCuentaPago(request.codcta(), authentication);
        if (cuentaPago.getEstado() != 1) {
            throw new BusinessRuleException("La CuentaPago seleccionada no está activa.");
        }
        return cuentaPago;
    }

    private void validateAvailableAmount(CuotaEntity cuota, BigDecimal monto) {
        BigDecimal confirmed = pagoRepository.sumConfirmedMontoByCuota(cuota.getCodcuo());
        if (confirmed.add(monto).compareTo(cuota.getMonto()) > 0) {
            throw new BusinessRuleException("El monto supera el saldo pendiente de la Cuota.");
        }
    }

    private void validateCuotaCanReceivePayment(CuotaEntity cuota) {
        if (cuota.getEstado() == CuotaEstado.ANULADA || cuota.getEstado() == CuotaEstado.PAGADA) {
            throw new BusinessRuleException("La Cuota no admite nuevos Pagos.");
        }
    }

    private void validatePending(PagoEntity pago, String action) {
        if (pago.getEstado() != PagoEstado.PENDIENTE_REVISION) {
            throw new BusinessRuleException("Solo se puede " + action + " un Pago pendiente de revisión.");
        }
    }

    private void validateCuentaForConfirmation(PagoEntity pago) {
        if (pago.getMetodo() != MetodoPago.EFECTIVO
                && (pago.getCuentaPago() == null || pago.getCuentaPago().getEstado() != 1)) {
            throw new BusinessRuleException("Transferencia y QR requieren una CuentaPago activa.");
        }
    }

    private void validateComprobanteForConfirmation(PagoEntity pago) {
        if (pago.getMetodo() != MetodoPago.EFECTIVO
                && !pagoComprobanteRepository.existsByPagoCodpag(pago.getCodpag())) {
            throw new BusinessRuleException("Transferencia y QR requieren al menos un comprobante antes de confirmar.");
        }
    }

    private CuotaEstado estadoCuota(BigDecimal montoCuota, BigDecimal montoConfirmado) {
        return montoConfirmado.compareTo(montoCuota) == 0 ? CuotaEstado.PAGADA : CuotaEstado.PARCIAL;
    }
}
