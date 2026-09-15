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
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.mapper.PagoMapper;
import com.orman.backend.payment.mapper.PagoComprobanteMapper;
import com.orman.backend.payment.mapper.ReciboMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.repository.ReciboRepository;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
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
    private final PagoComprobanteMapper pagoComprobanteMapper;
    private final ReciboMapper reciboMapper;
    private final PaymentOwnershipService paymentOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    @Override
    @Transactional
    public PagoResponse create(Integer codcuo, PagoRequest request, Authentication authentication) {
        RegistrationContext context = registrationContext(codcuo, authentication);
        CuotaEntity cuota = context.cuota();
        validateCuotaCanReceivePayment(cuota);
        validatePaymentAmount(request.monto());
        if (pagoRepository.findByIdempotencyKey(request.idempotencyKey()).isPresent()) {
            throw new ConflictException("La clave de idempotencia ya fue utilizada.");
        }
        validateOriginMethod(context.origenRegistro(), request.metodo(), request.comprobante());
        CuentaPagoEntity cuentaPago = validateCuentaPago(request, cuota);
        validateAvailableAmount(cuota, request.monto());
        try {
            PagoEntity pago = pagoRepository.saveAndFlush(pagoMapper.toEntity(request, cuota, cuentaPago,
                    context.origenRegistro(), context.actor(), now()));
            if (request.comprobante() != null) {
                pagoComprobanteRepository.saveAndFlush(pagoComprobanteMapper.toEntity(request.comprobante(), pago));
            }
            if (context.origenRegistro() == OrigenRegistroPago.PROPIETARIA) {
                confirmFinancially(pago, cuota, context.actor(), false);
            } else {
                applicationEventPublisher.publishEvent(new com.orman.backend.payment.event.ComprobanteRecibidoEvent(
                        pago.getCodpag()));
            }
            return pagoMapper.toResponse(pago);
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
        Usuario revisor = currentUser(authentication);
        confirmFinancially(pago, cuota, revisor, true);
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional
    public PagoResponse reject(Integer codpag, PagoMotivoRequest request, Authentication authentication) {
        PagoEntity pago = paymentOwnershipService.findOwnedPagoForUpdate(codpag, authentication);
        validatePending(pago, "rechazar");
        pago.setEstado(PagoEstado.RECHAZADO);
        pago.setFechaRevision(now());
        pago.setRevisadoPor(currentUser(authentication));
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
        pago.setFechaRevision(now());
        pago.setRevisadoPor(currentUser(authentication));
        pago.setMotivoRechazo(null);
        pago.setMotivoAnulacion(request.motivo().trim());
        return pagoMapper.toResponse(pago);
    }

    private CuentaPagoEntity validateCuentaPago(PagoRequest request, CuotaEntity cuota) {
        if (request.metodo() == MetodoPago.EFECTIVO) {
            if (request.codcta() != null) {
                throw new BusinessRuleException("Los Pagos en efectivo no deben indicar una CuentaPago.");
            }
            return null;
        }
        if (request.codcta() == null) {
            throw new BusinessRuleException("Transferencia y QR requieren una CuentaPago activa.");
        }
        CuentaPagoEntity cuentaPago = paymentOwnershipService.findCuentaPago(request.codcta());
        Integer propietariaCuota = cuota.getContrato().getUnidad().getPropiedad().getPropietaria().getCodper();
        if (!propietariaCuota.equals(cuentaPago.getPropietaria().getCodper())) {
            throw new AccessDeniedException("La CuentaPago no corresponde a la Propiedad de la Cuota.");
        }
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

    private void validateOriginMethod(OrigenRegistroPago origen, MetodoPago metodo,
                                      com.orman.backend.payment.dto.request.PagoComprobanteRequest comprobante) {
        if (origen == OrigenRegistroPago.INQUILINO) {
            if (metodo == MetodoPago.EFECTIVO) {
                throw new BusinessRuleException("El Inquilino no puede presentar Pagos en efectivo.");
            }
            if (comprobante == null) {
                throw new BusinessRuleException("El comprobante es obligatorio para un Pago presentado por el Inquilino.");
            }
        }
    }

    private void validatePaymentAmount(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || monto.scale() > 2) {
            throw new BusinessRuleException("El monto del Pago debe ser mayor a cero y tener máximo dos decimales.");
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
        if (pago.getOrigenRegistro() == OrigenRegistroPago.INQUILINO
                && !pagoComprobanteRepository.existsByPagoCodpag(pago.getCodpag())) {
            throw new BusinessRuleException("El Pago presentado por el Inquilino requiere un comprobante antes de confirmar.");
        }
    }

    private void confirmFinancially(PagoEntity pago, CuotaEntity cuota, Usuario revisor,
                                    boolean validatePresentedPayment) {
        validateCuotaCanReceivePayment(cuota);
        validateCuentaForConfirmation(pago);
        if (validatePresentedPayment) {
            validateComprobanteForConfirmation(pago);
        }
        BigDecimal confirmedBefore = pagoRepository.sumConfirmedMontoByCuota(cuota.getCodcuo());
        BigDecimal confirmedAfter = confirmedBefore.add(pago.getMonto());
        if (confirmedAfter.compareTo(cuota.getMonto()) > 0) {
            throw new BusinessRuleException("El Pago supera el saldo pendiente de la Cuota.");
        }
        pago.setEstado(PagoEstado.CONFIRMADO);
        pago.setFechaRevision(now());
        pago.setRevisadoPor(revisor);
        pago.setMotivoRechazo(null);
        pago.setMotivoAnulacion(null);
        cuota.setEstado(estadoCuota(cuota.getMonto(), confirmedAfter));
        cuotaRepository.saveAndFlush(cuota);
        pagoRepository.saveAndFlush(pago);
        if (reciboRepository.existsByPagoCodpag(pago.getCodpag())) {
            throw new ConflictException("El Pago ya tiene un Recibo generado.");
        }
        reciboRepository.saveAndFlush(reciboMapper.toEntity(pago, now()));
        applicationEventPublisher.publishEvent(new PagoConfirmadoEvent(pago.getCodpag()));
    }

    private RegistrationContext registrationContext(Integer codcuo, Authentication authentication) {
        Usuario actor = currentUser(authentication);
        if (hasRole(authentication, "ROLE_PROPIETARIO")) {
            return new RegistrationContext(paymentOwnershipService.findOwnedCuotaForUpdate(codcuo, authentication),
                    actor, OrigenRegistroPago.PROPIETARIA);
        }
        if (hasRole(authentication, "ROLE_INQUILINO")) {
            CuotaEntity cuota = paymentOwnershipService.findCuota(codcuo);
            if (!actor.getPersona().getCodper().equals(cuota.getContrato().getInquilino().getCodper())) {
                throw new AccessDeniedException("La Cuota no pertenece al Inquilino autenticado.");
            }
            return new RegistrationContext(cuota, actor, OrigenRegistroPago.INQUILINO);
        }
        throw new AccessDeniedException("No tiene autorización para registrar Pagos.");
    }

    private Usuario currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("No existe un Usuario autenticado válido.");
        }
        return usuarioRepository.findByLoginWithPersona(user.login())
                .orElseThrow(() -> new AccessDeniedException("El Usuario autenticado no existe."));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private CuotaEstado estadoCuota(BigDecimal montoCuota, BigDecimal montoConfirmado) {
        return montoConfirmado.compareTo(montoCuota) == 0 ? CuotaEstado.PAGADA : CuotaEstado.PARCIAL;
    }

    private record RegistrationContext(CuotaEntity cuota, Usuario actor, OrigenRegistroPago origenRegistro) {
    }
}
