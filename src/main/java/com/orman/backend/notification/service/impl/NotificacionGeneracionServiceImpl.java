package com.orman.backend.notification.service.impl;

import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.mapper.NotificacionMapper;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacionGeneracionServiceImpl implements NotificacionGeneracionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionGeneracionServiceImpl.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/La_Paz");
    private static final Locale SPANISH_BOLIVIA = Locale.forLanguageTag("es-BO");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("MMMM 'de' uuuu",
            SPANISH_BOLIVIA);
    private static final Short ACTIVE = 1;

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;
    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generatePaymentConfirmed(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createPaymentDecisionNotification(pago, NotificacionTipo.PAGO_CONFIRMADO, codpag,
                "Pago confirmado", "El pago de la cuota fue confirmado.");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generatePaymentRejected(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createPaymentDecisionNotification(pago, NotificacionTipo.PAGO_RECHAZADO, codpag,
                "Pago rechazado", "El comprobante enviado fue rechazado.");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateComprobanteReceived(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createIfAbsent(ownerUser(pago.getCuota()), NotificacionTipo.COMPROBANTE_RECIBIDO, ReferenciaTipo.PAGO, codpag,
                "Comprobante recibido", "Se registró un comprobante de pago para revisión.");
    }

    @Override
    @Transactional
    public void generateUpcomingQuota(Integer codcuo, LocalDate fechaActual) {
        CuotaEntity cuota = findCuota(codcuo);
        BigDecimal saldo = positiveBalance(cuota);
        if (saldo == null) {
            return;
        }
        Optional<Usuario> destinatario = findActiveTenantUser(cuota.getContrato().getInquilino());
        if (destinatario.isEmpty()) {
            logSkippedAutomaticNotification(codcuo);
            return;
        }
        String vencimiento = cuota.getFechaVencimiento().equals(fechaActual) ? "vence hoy." : "vence mañana.";
        String mensaje = "Tu cuota de " + cuota.getPeriodo().format(PERIOD_FORMAT)
                + " tiene un saldo pendiente de Bs " + formatAmount(saldo) + " y " + vencimiento;
        createIfAbsent(destinatario.get(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA, codcuo,
                "Pago próximo a vencer", mensaje);
    }

    @Override
    @Transactional
    public void generateOverdueQuota(Integer codcuo) {
        CuotaEntity cuota = findCuota(codcuo);
        BigDecimal saldo = positiveBalance(cuota);
        if (saldo == null) {
            return;
        }
        Optional<Usuario> destinatario = findActiveTenantUser(cuota.getContrato().getInquilino());
        if (destinatario.isEmpty()) {
            logSkippedAutomaticNotification(codcuo);
            return;
        }
        String mensaje = "Tu cuota de " + cuota.getPeriodo().format(PERIOD_FORMAT)
                + " tiene un saldo pendiente de Bs " + formatAmount(saldo) + " y se encuentra vencida.";
        createIfAbsent(destinatario.get(), NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA, codcuo,
                "Pago vencido", mensaje);
    }

    @Override
    @Transactional
    public NotificacionResponse notifyPendingPayment(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = findCuota(codcuo);
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                cuota.getContrato().getUnidad().getPropiedad().getPropietaria());
        if (cuota.getEstado() != CuotaEstado.PENDIENTE && cuota.getEstado() != CuotaEstado.PARCIAL) {
            throw new BusinessRuleException("Solo se puede notificar una Cuota pendiente o parcial con saldo.");
        }
        BigDecimal montoConfirmado = pagoRepository.sumConfirmedMontoByCuota(codcuo);
        BigDecimal saldo = cuota.getMonto().subtract(montoConfirmado);
        if (saldo.signum() <= 0) {
            throw new BusinessRuleException("No se puede notificar una Cuota sin saldo pendiente.");
        }

        Usuario destinatario = tenantUser(cuota.getContrato().getInquilino());
        NotificacionTipo tipo = cuota.getFechaVencimiento().isBefore(LocalDate.now(BUSINESS_ZONE))
                ? NotificacionTipo.CUOTA_VENCIDA : NotificacionTipo.CUOTA_PROXIMA_VENCER;
        String titulo = "Pago pendiente";
        String mensaje = manualPaymentMessage(cuota, montoConfirmado, saldo);
        NotificacionEntity notificacion = createOrRefreshManualReminder(destinatario, tipo, codcuo, titulo, mensaje);
        return notificacionMapper.toResponse(notificacion);
    }

    private NotificacionEntity createOrRefreshManualReminder(Usuario destinatario, NotificacionTipo tipo,
                                                              Integer codcuo, String titulo, String mensaje) {
        NotificacionEntity notificacion = notificacionRepository
                .findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                        destinatario.getLogin(), tipo, ReferenciaTipo.CUOTA, codcuo)
                .orElse(null);
        if (notificacion == null) {
            return notificacionRepository.saveAndFlush(notificacionMapper.toEntity(destinatario, tipo, titulo, mensaje,
                    ReferenciaTipo.CUOTA, codcuo));
        }

        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setFechaLectura(null);
        return notificacionRepository.saveAndFlush(notificacion);
    }

    private String manualPaymentMessage(CuotaEntity cuota, BigDecimal montoConfirmado, BigDecimal saldo) {
        String periodo = cuota.getPeriodo().format(PERIOD_FORMAT);
        if (montoConfirmado.signum() == 0) {
            return "Tienes pendiente el pago de Bs " + formatAmount(cuota.getMonto())
                    + " correspondiente a " + periodo + ".";
        }
        return "Tienes un saldo pendiente de Bs " + formatAmount(saldo)
                + " correspondiente a " + periodo + ".";
    }

    private String formatAmount(BigDecimal amount) {
        NumberFormat amountFormat = NumberFormat.getNumberInstance(SPANISH_BOLIVIA);
        amountFormat.setMinimumFractionDigits(2);
        amountFormat.setMaximumFractionDigits(2);
        return amountFormat.format(amount);
    }

    private BigDecimal positiveBalance(CuotaEntity cuota) {
        if (cuota.getEstado() != CuotaEstado.PENDIENTE && cuota.getEstado() != CuotaEstado.PARCIAL) {
            return null;
        }
        BigDecimal confirmed = pagoRepository.sumConfirmedMontoByCuota(cuota.getCodcuo());
        BigDecimal saldo = cuota.getMonto().subtract(confirmed);
        return saldo.signum() > 0 ? saldo : null;
    }

    private void logSkippedAutomaticNotification(Integer codcuo) {
        LOGGER.warn("Se omite la notificación automática de la cuota {}: el inquilino no tiene un usuario activo válido.",
                codcuo);
    }

    private NotificacionEntity createIfAbsent(Usuario destinatario, NotificacionTipo tipo,
                                              ReferenciaTipo referenciaTipo, Integer referenciaId,
                                              String titulo, String mensaje) {
        return notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                destinatario.getLogin(), tipo, referenciaTipo, referenciaId)
                .orElseGet(() -> notificacionRepository.saveAndFlush(
                        notificacionMapper.toEntity(destinatario, tipo, titulo, mensaje, referenciaTipo, referenciaId)));
    }

    private void createPaymentDecisionNotification(PagoEntity pago, NotificacionTipo tipo, Integer codpag,
                                                   String titulo, String mensaje) {
        Usuario destinatario;
        if (pago.getOrigenRegistro() == OrigenRegistroPago.INQUILINO) {
            Optional<Usuario> activeTenant = findActiveTenantUser(pago.getCuota().getContrato().getInquilino());
            if (activeTenant.isEmpty()) {
                logSkippedPaymentNotification(codpag);
                return;
            }
            destinatario = activeTenant.get();
        } else {
            // Preserve the existing notification for payments entered directly by the owner.
            destinatario = ownerUser(pago.getCuota());
        }
        createIfAbsent(destinatario, tipo, ReferenciaTipo.PAGO, codpag, titulo, mensaje);
    }

    private CuotaEntity findCuota(Integer codcuo) {
        return cuotaRepository.findByCodcuo(codcuo)
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada."));
    }

    private PagoEntity findPago(Integer codpag) {
        return pagoRepository.findByCodpag(codpag)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado."));
    }

    private Usuario ownerUser(CuotaEntity cuota) {
        Integer codper = cuota.getContrato().getUnidad().getPropiedad().getPropietaria().getCodper();
        return usuarioRepository.findByPersonaCodper(codper)
                .orElseThrow(() -> new BusinessRuleException("La Persona propietaria no tiene un Usuario asociado."));
    }

    private Usuario tenantUser(Persona inquilino) {
        Usuario usuario = findTenantUser(inquilino)
                .orElseThrow(() -> new BusinessRuleException("El Inquilino no tiene un Usuario asociado."));
        if (!isActiveTenantUser(inquilino, usuario)) {
            throw new BusinessRuleException("El Inquilino no tiene un Usuario activo asociado.");
        }
        return usuario;
    }

    private Optional<Usuario> findActiveTenantUser(Persona inquilino) {
        return findTenantUser(inquilino).filter(usuario -> isActiveTenantUser(inquilino, usuario));
    }

    private Optional<Usuario> findTenantUser(Persona inquilino) {
        if (inquilino == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByPersonaCodper(inquilino.getCodper());
    }

    private boolean isActiveTenantUser(Persona inquilino, Usuario usuario) {
        return ACTIVE.equals(usuario.getEstado()) && ACTIVE.equals(inquilino.getEstado())
                && Character.valueOf('I').equals(inquilino.getTipoPersona());
    }

    private void logSkippedPaymentNotification(Integer codpag) {
        LOGGER.warn("Se omite aviso de pago {}: el inquilino no tiene un usuario activo valido.", codpag);
    }
}
