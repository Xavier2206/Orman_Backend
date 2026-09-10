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
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacionGeneracionServiceImpl implements NotificacionGeneracionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;
    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional
    public void generatePaymentConfirmed(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createIfAbsent(ownerUser(pago.getCuota()), NotificacionTipo.PAGO_CONFIRMADO, ReferenciaTipo.PAGO, codpag,
                "Pago confirmado", "El pago de la cuota fue confirmado. Su recibo se encuentra disponible.");
    }

    @Override
    @Transactional
    public void generatePaymentRejected(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createIfAbsent(ownerUser(pago.getCuota()), NotificacionTipo.PAGO_RECHAZADO, ReferenciaTipo.PAGO, codpag,
                "Pago rechazado", "El comprobante enviado fue rechazado.");
    }

    @Override
    @Transactional
    public void generateComprobanteReceived(Integer codpag) {
        PagoEntity pago = findPago(codpag);
        createIfAbsent(ownerUser(pago.getCuota()), NotificacionTipo.COMPROBANTE_RECIBIDO, ReferenciaTipo.PAGO, codpag,
                "Comprobante recibido", "Se registró un comprobante de pago para revisión.");
    }

    @Override
    @Transactional
    public void generateUpcomingQuota(Integer codcuo, LocalDate fechaActual) {
        CuotaEntity cuota = findCuota(codcuo);
        String unidad = cuota.getContrato().getUnidad().getNombre();
        String propiedad = cuota.getContrato().getUnidad().getPropiedad().getNombre();
        String mensaje = cuota.getFechaVencimiento().equals(fechaActual)
                ? "La cuota de alquiler del " + unidad + " vence hoy."
                : "La cuota de alquiler del " + unidad + " del " + propiedad + " vence mañana.";
        createIfAbsent(ownerUser(cuota), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA, codcuo,
                "Cuota próxima a vencer", mensaje);
    }

    @Override
    @Transactional
    public void generateOverdueQuota(Integer codcuo) {
        CuotaEntity cuota = findCuota(codcuo);
        String unidad = cuota.getContrato().getUnidad().getNombre();
        createIfAbsent(ownerUser(cuota), NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA, codcuo,
                "Cuota vencida", "La cuota de alquiler del " + unidad + " se encuentra vencida.");
    }

    @Override
    @Transactional
    public NotificacionResponse notifyPendingPayment(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = findCuota(codcuo);
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                cuota.getContrato().getUnidad().getPropiedad().getPropietaria());
        if (cuota.getEstado() != CuotaEstado.PENDIENTE && cuota.getEstado() != CuotaEstado.PARCIAL) {
            throw new BusinessRuleException("Solo se puede notificar una Cuota pendiente, parcial o vencida.");
        }
        NotificacionTipo tipo = cuota.getFechaVencimiento().isBefore(LocalDate.now(ZoneId.of("America/La_Paz")))
                ? NotificacionTipo.CUOTA_VENCIDA : NotificacionTipo.CUOTA_PROXIMA_VENCER;
        NotificacionEntity notificacion = createIfAbsent(ownerUser(cuota), tipo, ReferenciaTipo.CUOTA, codcuo,
                "Recordatorio de pago", "Le recordamos que su cuota de alquiler se encuentra pendiente de pago.");
        return notificacionMapper.toResponse(notificacion);
    }

    private NotificacionEntity createIfAbsent(Usuario destinatario, NotificacionTipo tipo,
                                              ReferenciaTipo referenciaTipo, Integer referenciaId,
                                              String titulo, String mensaje) {
        return notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                destinatario.getLogin(), tipo, referenciaTipo, referenciaId)
                .orElseGet(() -> notificacionRepository.saveAndFlush(
                        notificacionMapper.toEntity(destinatario, tipo, titulo, mensaje, referenciaTipo, referenciaId)));
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
}
