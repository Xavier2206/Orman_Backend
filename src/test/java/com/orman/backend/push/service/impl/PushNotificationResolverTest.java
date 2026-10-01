package com.orman.backend.push.service.impl;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.push.service.PushNotificationMessage;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushNotificationResolverTest {

    private static final Integer CODCUO = 72;
    private static final Integer CODPAG = 81;
    private static final String TENANT_LOGIN = "tenant.mobile";

    private NotificacionRepository notificationRepository;
    private PagoRepository paymentRepository;
    private CuotaRepository quotaRepository;
    private UsuarioRepository userRepository;
    private PushNotificationResolver resolver;
    private CuotaEntity quota;
    private Usuario tenant;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificacionRepository.class);
        paymentRepository = mock(PagoRepository.class);
        quotaRepository = mock(CuotaRepository.class);
        userRepository = mock(UsuarioRepository.class);
        resolver = new PushNotificationResolver(notificationRepository, paymentRepository, quotaRepository,
                userRepository);

        Persona tenantPerson = new Persona();
        ReflectionTestUtils.setField(tenantPerson, "codper", 14);
        tenant = new Usuario();
        tenant.setLogin(TENANT_LOGIN);
        quota = quota(CODCUO, tenantPerson);
        when(userRepository.findByPersonaCodper(14)).thenReturn(Optional.of(tenant));
    }

    @Test
    void resolvesBothPaymentDecisionTypesFromPaymentToQuota() {
        for (NotificacionTipo type : new NotificacionTipo[] {
                NotificacionTipo.PAGO_CONFIRMADO, NotificacionTipo.PAGO_RECHAZADO}) {
            NotificacionEntity notification = notification(100L + type.ordinal(), type, ReferenciaTipo.PAGO, CODPAG);
            PagoEntity payment = new PagoEntity();
            payment.setOrigenRegistro(OrigenRegistroPago.INQUILINO);
            payment.setCuota(quota);
            when(notificationRepository.findForPush(notification.getCodnot())).thenReturn(Optional.of(notification));
            when(paymentRepository.findByCodpag(CODPAG)).thenReturn(Optional.of(payment));

            PushNotificationMessage message = resolver.find(notification.getCodnot()).orElseThrow();

            assertThat(message.loginDestinatario()).isEqualTo(TENANT_LOGIN);
            assertThat(message.data()).containsEntry("tipo", type.name())
                    .containsEntry("referenciaTipo", "PAGO")
                    .containsEntry("referenciaId", CODPAG.toString())
                    .containsEntry("codcuo", CODCUO.toString());
        }
    }

    @Test
    void quotaNotificationsUseTheirQuotaReferenceDirectlyAndKeepTextGeneric() {
        for (NotificacionTipo type : new NotificacionTipo[] {
                NotificacionTipo.CUOTA_PROXIMA_VENCER, NotificacionTipo.CUOTA_VENCIDA}) {
            NotificacionEntity notification = notification(200L + type.ordinal(), type,
                    ReferenciaTipo.CUOTA, CODCUO);
            when(notificationRepository.findForPush(notification.getCodnot())).thenReturn(Optional.of(notification));
            when(quotaRepository.findByCodcuo(CODCUO)).thenReturn(Optional.of(quota));

            PushNotificationMessage message = resolver.find(notification.getCodnot()).orElseThrow();

            assertThat(message.title()).isEqualTo("ORMAN");
            assertThat(message.data()).containsEntry("codcuo", CODCUO.toString())
                    .containsEntry("referenciaId", CODCUO.toString());
            assertThat(message.body()).doesNotContain("Bs", "CI", "saldo", "dirección");
            assertThat(message.data().values()).allMatch(String.class::isInstance);
        }
        verify(paymentRepository, never()).findByCodpag(CODPAG);
    }

    @Test
    void doesNotSendReceiptOrOwnerPaymentDecisionToTheMobileTenant() {
        NotificacionEntity receipt = notification(301L, NotificacionTipo.COMPROBANTE_RECIBIDO,
                ReferenciaTipo.PAGO, CODPAG);
        when(notificationRepository.findForPush(301L)).thenReturn(Optional.of(receipt));

        assertThat(resolver.find(301L)).isEmpty();
        verify(paymentRepository, never()).findByCodpag(CODPAG);

        NotificacionEntity ownerDecision = notification(302L, NotificacionTipo.PAGO_CONFIRMADO,
                ReferenciaTipo.PAGO, CODPAG);
        PagoEntity payment = new PagoEntity();
        payment.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        payment.setCuota(quota);
        when(notificationRepository.findForPush(302L)).thenReturn(Optional.of(ownerDecision));
        when(paymentRepository.findByCodpag(CODPAG)).thenReturn(Optional.of(payment));

        assertThat(resolver.find(302L)).isEmpty();
    }

    @Test
    void omitsMissingPaymentAndRecipientMismatchWithoutThrowing() {
        NotificacionEntity missing = notification(401L, NotificacionTipo.PAGO_RECHAZADO,
                ReferenciaTipo.PAGO, CODPAG);
        when(notificationRepository.findForPush(401L)).thenReturn(Optional.of(missing));
        when(paymentRepository.findByCodpag(CODPAG)).thenReturn(Optional.empty());
        assertThat(resolver.find(401L)).isEmpty();

        Persona otherPerson = new Persona();
        ReflectionTestUtils.setField(otherPerson, "codper", 15);
        NotificacionEntity wrongRecipient = notification(402L, NotificacionTipo.CUOTA_VENCIDA,
                ReferenciaTipo.CUOTA, CODCUO);
        wrongRecipient.setDestinatario(user("owner.mobile"));
        when(notificationRepository.findForPush(402L)).thenReturn(Optional.of(wrongRecipient));
        when(quotaRepository.findByCodcuo(CODCUO)).thenReturn(Optional.of(quota(CODCUO, otherPerson)));
        when(userRepository.findByPersonaCodper(15)).thenReturn(Optional.of(user("tenant.mobile")));

        assertThat(resolver.find(402L)).isEmpty();
    }

    private NotificacionEntity notification(Long codnot, NotificacionTipo type, ReferenciaTipo referenceType,
                                            Integer referenceId) {
        NotificacionEntity notification = new NotificacionEntity();
        ReflectionTestUtils.setField(notification, "codnot", codnot);
        notification.setDestinatario(tenant);
        notification.setTipo(type);
        notification.setReferenciaTipo(referenceType);
        notification.setReferenciaId(referenceId);
        return notification;
    }

    private CuotaEntity quota(Integer codcuo, Persona tenantPerson) {
        CuotaEntity newQuota = new CuotaEntity();
        ReflectionTestUtils.setField(newQuota, "codcuo", codcuo);
        ContratoEntity contract = new ContratoEntity();
        contract.setInquilino(tenantPerson);
        newQuota.setContrato(contract);
        return newQuota;
    }

    private Usuario user(String login) {
        Usuario user = new Usuario();
        user.setLogin(login);
        return user;
    }
}
