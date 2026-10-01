package com.orman.backend.push.service.impl;

import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.push.service.PushNotificationMessage;
import com.orman.backend.user.repository.UsuarioRepository;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushNotificationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushNotificationResolver.class);
    private static final String TITLE = "ORMAN";

    private final NotificacionRepository notificationRepository;
    private final PagoRepository paymentRepository;
    private final CuotaRepository quotaRepository;
    private final UsuarioRepository userRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<PushNotificationMessage> find(Long codnot) {
        return notificationRepository.findForPush(codnot).flatMap(this::resolve);
    }

    private Optional<PushNotificationMessage> resolve(NotificacionEntity notification) {
        return switch (notification.getTipo()) {
            case CUOTA_PROXIMA_VENCER, CUOTA_VENCIDA -> resolveQuotaNotification(notification);
            case PAGO_CONFIRMADO, PAGO_RECHAZADO -> resolvePaymentNotification(notification);
            case COMPROBANTE_RECIBIDO -> Optional.empty();
        };
    }

    private Optional<PushNotificationMessage> resolveQuotaNotification(NotificacionEntity notification) {
        if (notification.getReferenciaTipo() != ReferenciaTipo.CUOTA) {
            return invalidReference(notification);
        }
        Optional<CuotaEntity> quota = quotaRepository.findByCodcuo(notification.getReferenciaId());
        return quota.map(entity -> createMessage(notification, entity))
                .orElseGet(() -> invalidReference(notification));
    }

    private Optional<PushNotificationMessage> resolvePaymentNotification(NotificacionEntity notification) {
        if (notification.getReferenciaTipo() != ReferenciaTipo.PAGO) {
            return invalidReference(notification);
        }
        Optional<PagoEntity> payment = paymentRepository.findByCodpag(notification.getReferenciaId());
        if (payment.isEmpty()) {
            return invalidReference(notification);
        }
        if (payment.get().getOrigenRegistro() != OrigenRegistroPago.INQUILINO) {
            return Optional.empty();
        }
        CuotaEntity quota = payment.get().getCuota();
        if (quota == null) {
            return invalidReference(notification);
        }
        return createMessage(notification, quota);
    }

    private Optional<PushNotificationMessage> createMessage(NotificacionEntity notification, CuotaEntity quota) {
        if (quota == null || quota.getCodcuo() == null || quota.getContrato() == null
                || quota.getContrato().getInquilino() == null
                || quota.getContrato().getInquilino().getCodper() == null) {
            return invalidReference(notification);
        }

        String tenantLogin = userRepository.findByPersonaCodper(quota.getContrato().getInquilino().getCodper())
                .map(user -> user.getLogin())
                .orElse(null);
        if (tenantLogin == null || !tenantLogin.equals(notification.getDestinatario().getLogin())) {
            return Optional.empty();
        }

        String body = switch (notification.getTipo()) {
            case PAGO_CONFIRMADO -> "Tu pago fue confirmado. Toca para consultar la cuota.";
            case PAGO_RECHAZADO -> "Tu pago requiere revisión. Toca para consultar la cuota.";
            case CUOTA_PROXIMA_VENCER -> "Tienes una cuota próxima a vencer.";
            case CUOTA_VENCIDA -> "Tienes una cuota vencida.";
            case COMPROBANTE_RECIBIDO -> throw new IllegalStateException("Unsupported mobile notification type");
        };
        Map<String, String> data = Map.of(
                "tipo", notification.getTipo().name(),
                "codnot", notification.getCodnot().toString(),
                "referenciaTipo", notification.getReferenciaTipo().name(),
                "referenciaId", notification.getReferenciaId().toString(),
                "codcuo", quota.getCodcuo().toString());
        return Optional.of(new PushNotificationMessage(notification.getCodnot(), tenantLogin,
                notification.getTipo(), TITLE, body, data));
    }

    private Optional<PushNotificationMessage> invalidReference(NotificacionEntity notification) {
        LOGGER.warn("Se omite el push de notificación {}: referencia inválida para tipo {}",
                notification.getCodnot(), notification.getTipo());
        return Optional.empty();
    }
}
