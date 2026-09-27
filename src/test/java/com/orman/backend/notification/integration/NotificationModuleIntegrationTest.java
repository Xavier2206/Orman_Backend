package com.orman.backend.notification.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.NotificacionEntity;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.mapper.NotificacionMapper;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.notification.service.NotificacionService;
import com.orman.backend.notification.service.impl.CuotaNotificacionScheduler;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoComprobanteEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class NotificationModuleIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private NotificacionMapper notificacionMapper;
    @Autowired private NotificacionService notificacionService;
    @Autowired private NotificacionGeneracionService notificacionGeneracionService;
    @Autowired private CuotaNotificacionScheduler cuotaNotificacionScheduler;
    @Autowired private PagoService pagoService;
    @Autowired private PagoComprobanteRepository pagoComprobanteRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesNotificationTableWithRequiredConstraintsAndIndexes() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class)).contains("notificaciones");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'notificaciones'::regclass", String.class))
                .contains("pk_notificaciones", "fk_notificaciones_usuarios_destinatario",
                        "uk_notificaciones_destinatario_tipo_referencia", "ck_notificaciones_tipo");
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'notificaciones'",
                String.class)).contains("ix_notificaciones_destinatario_fecha_creacion",
                        "ix_notificaciones_destinatario_no_leidas");
    }

    @Test
    void schedulerGeneratesUpcomingTodayAndOverdueNotificationsOnlyOnce() {
        Context context = context("SCH");
        Usuario tenant = user("not.scheduler.tenant." + UUID.randomUUID(), context.contrato().getInquilino());
        CuotaEntity overdue = cuota(context, LocalDate.of(2026, 9, 1), CuotaEstado.PARCIAL);
        CuotaEntity tomorrow = cuota(context, LocalDate.of(2026, 10, 1), CuotaEstado.PENDIENTE);
        CuotaEntity today = cuota(context, LocalDate.of(2026, 11, 1), CuotaEstado.PENDIENTE);
        CuotaEntity paid = cuota(context, LocalDate.of(2026, 8, 1), CuotaEstado.PAGADA);
        CuotaEntity annulled = cuota(context, LocalDate.of(2026, 7, 1), CuotaEstado.ANULADA);
        Context noBalanceContext = context("SCH-NO-BALANCE");
        Usuario noBalanceTenant = user("not.scheduler.no.balance." + UUID.randomUUID(),
                noBalanceContext.contrato().getInquilino());
        CuotaEntity noBalance = cuota(noBalanceContext, LocalDate.of(2026, 10, 1), CuotaEstado.PENDIENTE);
        pagoService.create(noBalance.getCodcuo(), paymentRequest(), null,
                authentication(noBalanceContext.usuario()));
        noBalance.setEstado(CuotaEstado.PENDIENTE);
        cuotaRepository.saveAndFlush(noBalance);

        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 9, 30));
        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 9, 30));

        assertNotification(tenant, NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA,
                overdue.getCodcuo());
        assertNotification(tenant, NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                tomorrow.getCodcuo());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, paid.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, annulled.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                noBalanceTenant.getLogin(), ReferenciaTipo.CUOTA, noBalance.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, overdue.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, tomorrow.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.searchOwn(tenant.getLogin(), null, null,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(2);
        assertThat(notificacionRepository.searchOwn(context.usuario().getLogin(), null, null,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isZero();

        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 11, 1));
        assertThat(notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                today.getCodcuo()).orElseThrow().getMensaje()).contains("vence hoy");
    }

    @Test
    void schedulerUsesConfirmedBalanceForPartialQuotaAndIgnoresPendingReviewAmount() {
        Context context = context("SCH-PARTIAL");
        Usuario tenant = user("not.sch.partial." + UUID.randomUUID(), context.contrato().getInquilino());
        LocalDate dueTomorrow = LocalDate.of(2026, 10, 1);
        LocalDate today = dueTomorrow.minusDays(1);
        CuotaEntity partial = cuota(context, dueTomorrow, CuotaEstado.PENDIENTE, new BigDecimal("2500.00"));

        pagoService.create(partial.getCodcuo(), paymentRequest(new BigDecimal("1000.00")), null,
                authentication(context.usuario()));
        pendingPayment(context, partial, new BigDecimal("500.00"));

        cuotaNotificacionScheduler.generateFor(today);

        NotificacionEntity notification = notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                partial.getCodcuo()).orElseThrow();
        assertThat(notification.getMensaje()).contains("1.500", periodDescription(dueTomorrow));
        assertThat(notification.getMensaje()).doesNotContain("2.000");
        assertThat(pagoRepository.sumConfirmedMontoByCuota(partial.getCodcuo())).isEqualByComparingTo("1000.00");
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, partial.getCodcuo())).isEmpty();
    }

    @Test
    void schedulerSkipsMissingTenantUserAndContinuesProcessingOtherQuotas() {
        LocalDate dueTomorrow = LocalDate.of(2026, 10, 1);
        LocalDate today = dueTomorrow.minusDays(1);
        Context missingUserContext = context("SCH-NO-USER");
        CuotaEntity missingUserQuota = cuota(missingUserContext, dueTomorrow, CuotaEstado.PENDIENTE);
        Context validContext = context("SCH-VALID");
        Usuario tenant = user("not.sch.valid." + UUID.randomUUID(), validContext.contrato().getInquilino());
        CuotaEntity validQuota = cuota(validContext, dueTomorrow, CuotaEstado.PENDIENTE);

        cuotaNotificacionScheduler.generateFor(today);

        assertNotification(tenant, NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                validQuota.getCodcuo());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                missingUserContext.usuario().getLogin(), ReferenciaTipo.CUOTA, missingUserQuota.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, missingUserQuota.getCodcuo())).isEmpty();
    }

    @Test
    void schedulerAllowsUpcomingAndOverdueTypesAndDoesNotReopenReadNotifications() {
        Context context = context("SCH-TRANSITION");
        Usuario tenant = user("not.sch.transition." + UUID.randomUUID(), context.contrato().getInquilino());
        LocalDate dueTomorrow = LocalDate.of(2026, 10, 1);
        CuotaEntity quota = cuota(context, dueTomorrow, CuotaEstado.PENDIENTE);

        cuotaNotificacionScheduler.generateFor(dueTomorrow.minusDays(1));
        NotificacionEntity upcoming = notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA, quota.getCodcuo())
                .orElseThrow();
        var read = notificacionService.markAsRead(upcoming.getCodnot(), authentication(tenant, "ROLE_INQUILINO"));

        cuotaNotificacionScheduler.generateFor(dueTomorrow.minusDays(1));
        cuotaNotificacionScheduler.generateFor(dueTomorrow.minusDays(1));
        cuotaNotificacionScheduler.generateFor(dueTomorrow.plusDays(1));
        cuotaNotificacionScheduler.generateFor(dueTomorrow.plusDays(1));

        NotificacionEntity overdue = notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA, quota.getCodcuo())
                .orElseThrow();
        NotificacionEntity unchangedUpcoming = notificacionRepository.findById(upcoming.getCodnot()).orElseThrow();
        assertThat(overdue.getCodnot()).isNotEqualTo(upcoming.getCodnot());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, quota.getCodcuo())).hasSize(2);
        assertThat(unchangedUpcoming.getFechaLectura()).isEqualTo(read.fechaLectura());
        assertThat(unchangedUpcoming.getFechaCreacion()).isEqualTo(upcoming.getFechaCreacion());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, quota.getCodcuo())).isEmpty();
    }

    @Test
    void createsManualReminderOnlyForOwnedPendingOrPartialQuotaAndAvoidsDuplicates() {
        Context context = context("MANUAL");
        Usuario tenant = user("not.manual.tenant." + UUID.randomUUID(), context.contrato().getInquilino());
        LocalDate pendingPeriod = LocalDate.now(ZoneId.of("America/La_Paz")).plusMonths(1).withDayOfMonth(1);
        CuotaEntity pending = cuota(context, pendingPeriod, CuotaEstado.PENDIENTE,
                new BigDecimal("2500.00"));
        Authentication ownerAuthentication = authentication(context.usuario());

        var first = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);
        NotificacionEntity stored = notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA, pending.getCodcuo())
                .orElseThrow();
        var creationTime = stored.getFechaCreacion();
        var repeated = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);

        assertThat(first.codnot()).isEqualTo(repeated.codnot());
        assertThat(first.referenciaTipo()).isEqualTo("CUOTA");
        assertThat(first.tipo()).isEqualTo("CUOTA_PROXIMA_VENCER");
        assertThat(first.titulo()).isEqualTo("Pago pendiente");
        assertThat(first.mensaje()).contains("Bs ", "2.500", periodDescription(pendingPeriod));
        assertThat(stored.getDestinatario().getLogin()).isEqualTo(tenant.getLogin());
        assertThat(stored.getReferenciaId()).isEqualTo(pending.getCodcuo());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, pending.getCodcuo())).hasSize(1);
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, pending.getCodcuo())).isEmpty();
        assertThat(repeated.leida()).isFalse();
        assertThat(stored.getFechaCreacion()).isEqualTo(creationTime);

        var read = notificacionService.markAsRead(first.codnot(), authentication(tenant, "ROLE_INQUILINO"));
        assertThat(read.leida()).isTrue();
        var reopened = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);
        NotificacionEntity reopenedEntity = notificacionRepository.findById(first.codnot()).orElseThrow();
        assertThat(reopened.codnot()).isEqualTo(first.codnot());
        assertThat(reopened.leida()).isFalse();
        assertThat(reopened.fechaLectura()).isNull();
        assertThat(reopenedEntity.getFechaCreacion()).isEqualTo(creationTime);

        Context other = context("OTHER");
        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(),
                authentication(other.usuario()))).isInstanceOf(AccessDeniedException.class);
        CuotaEntity paid = cuota(context, pendingPeriod.plusMonths(1), CuotaEstado.PAGADA);
        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(paid.getCodcuo(), ownerAuthentication))
                .isInstanceOf(BusinessRuleException.class);
        CuotaEntity annulled = cuota(context, pendingPeriod.plusMonths(2), CuotaEstado.ANULADA);
        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(annulled.getCodcuo(), ownerAuthentication))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void manualReminderUsesConfirmedBalanceAndDoesNotSubtractPendingReviewPayments() {
        Context context = context("MANUAL-PARTIAL");
        Usuario tenant = user("not.partial.tenant." + UUID.randomUUID(), context.contrato().getInquilino());
        LocalDate pastPeriod = LocalDate.now(ZoneId.of("America/La_Paz")).minusMonths(1).withDayOfMonth(1);
        CuotaEntity partial = cuota(context, pastPeriod, CuotaEstado.PENDIENTE,
                new BigDecimal("2500.00"));
        Authentication ownerAuthentication = authentication(context.usuario());

        pagoService.create(partial.getCodcuo(), paymentRequest(new BigDecimal("1000.00")), null, ownerAuthentication);
        assertThat(cuotaRepository.findByCodcuo(partial.getCodcuo()).orElseThrow().getEstado())
                .isEqualTo(CuotaEstado.PARCIAL);
        pendingPayment(context, partial, new BigDecimal("500.00"));

        var response = notificacionGeneracionService.notifyPendingPayment(partial.getCodcuo(), ownerAuthentication);
        NotificacionEntity stored = notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA, partial.getCodcuo())
                .orElseThrow();

        assertThat(response.tipo()).isEqualTo("CUOTA_VENCIDA");
        assertThat(response.mensaje()).contains("Bs ", "1.500", periodDescription(pastPeriod));
        assertThat(response.mensaje()).doesNotContain("2.500");
        assertThat(pagoRepository.sumConfirmedMontoByCuota(partial.getCodcuo()))
                .isEqualByComparingTo("1000.00");
        assertThat(stored.getDestinatario().getLogin()).isEqualTo(tenant.getLogin());
    }

    @Test
    void manualReminderRejectsNonPositiveBalanceEvenIfQuotaStateIsInconsistent() {
        Context context = context("MANUAL-ZERO");
        Usuario tenant = user("not.zero.tenant." + UUID.randomUUID(), context.contrato().getInquilino());
        CuotaEntity quota = cuota(context, LocalDate.of(2026, 9, 1), CuotaEstado.PENDIENTE);
        Authentication ownerAuthentication = authentication(context.usuario());
        pagoService.create(quota.getCodcuo(), paymentRequest(), null, ownerAuthentication);
        quota.setEstado(CuotaEstado.PENDIENTE);
        cuotaRepository.saveAndFlush(quota);

        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(quota.getCodcuo(), ownerAuthentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede notificar una Cuota sin saldo pendiente.");
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                tenant.getLogin(), ReferenciaTipo.CUOTA, quota.getCodcuo())).isEmpty();
    }

    @Test
    void manualReminderRequiresAnActiveTenantUser() {
        Context context = context("MANUAL-NO-USER");
        CuotaEntity pending = cuota(context, LocalDate.of(2026, 12, 1), CuotaEstado.PENDIENTE);

        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(
                pending.getCodcuo(), authentication(context.usuario())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El Inquilino no tiene un Usuario asociado.");
    }

    @Test
    void listsOwnNotificationsAndMarksThemReadIdempotently() {
        Context context = context("READ");
        Usuario tenant = user("not.read.tenant." + UUID.randomUUID(), context.contrato().getInquilino());
        CuotaEntity pending = cuota(context, LocalDate.of(2026, 12, 1), CuotaEstado.PENDIENTE);
        Authentication ownerAuthentication = authentication(context.usuario());
        var created = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);
        Authentication tenantAuthentication = authentication(tenant, "ROLE_INQUILINO");

        assertThat(notificacionService.list(null, false,
                org.springframework.data.domain.PageRequest.of(0, 20), tenantAuthentication).content())
                .extracting(response -> response.codnot()).contains(created.codnot());
        assertThat(notificacionService.list(null, false,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication).content()).isEmpty();
        assertThat(notificacionService.summary(tenantAuthentication).noLeidas()).isEqualTo(1);
        var firstRead = notificacionService.markAsRead(created.codnot(), tenantAuthentication);
        var repeatedRead = notificacionService.markAsRead(created.codnot(), tenantAuthentication);
        assertThat(firstRead.leida()).isTrue();
        assertThat(repeatedRead.fechaLectura()).isEqualTo(firstRead.fechaLectura());
        assertThat(notificacionService.summary(tenantAuthentication).noLeidas()).isZero();

        Context other = context("READ-OTHER");
        Usuario otherTenant = user("not.read.other." + UUID.randomUUID(), other.contrato().getInquilino());
        assertThatThrownBy(() -> notificacionService.get(created.codnot(),
                authentication(otherTenant, "ROLE_INQUILINO")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tenantNotificationReadsRemainIsolatedByAuthenticatedLogin() {
        Context context = context("TENANT-READ");
        Usuario tenantA = user("notification.tenant.a." + UUID.randomUUID(), context.contrato().getInquilino());
        Persona tenantBPerson = persona("NOTIFY-TENANT-B-" + UUID.randomUUID());
        Usuario tenantB = user("notification.tenant.b." + UUID.randomUUID(), tenantBPerson);
        NotificacionEntity tenantANotification = notification(tenantA, NotificacionTipo.CUOTA_VENCIDA, 71);
        NotificacionEntity tenantASecondNotification = notification(tenantA, NotificacionTipo.CUOTA_PROXIMA_VENCER, 72);
        NotificacionEntity tenantBNotification = notification(tenantB, NotificacionTipo.CUOTA_VENCIDA, 73);
        NotificacionEntity ownerNotification = notification(context.usuario(), NotificacionTipo.PAGO_CONFIRMADO, 74,
                ReferenciaTipo.PAGO);
        Authentication tenantAAuthentication = authentication(tenantA, "ROLE_INQUILINO");

        var ownPage = notificacionService.list(null, null,
                org.springframework.data.domain.PageRequest.of(0, 20), tenantAAuthentication);

        assertThat(ownPage.content()).extracting(row -> row.codnot())
                .containsExactlyInAnyOrder(tenantANotification.getCodnot(), tenantASecondNotification.getCodnot())
                .doesNotContain(tenantBNotification.getCodnot(), ownerNotification.getCodnot());
        assertThat(notificacionService.summary(tenantAAuthentication).noLeidas()).isEqualTo(2);
        assertThat(notificacionService.get(tenantANotification.getCodnot(), tenantAAuthentication).referenciaTipo())
                .isEqualTo("CUOTA");
        assertThat(notificacionService.markAsRead(tenantANotification.getCodnot(), tenantAAuthentication).leida())
                .isTrue();
        assertThat(notificacionService.summary(tenantAAuthentication).noLeidas()).isEqualTo(1);
        assertThatThrownBy(() -> notificacionService.get(tenantBNotification.getCodnot(), tenantAAuthentication))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> notificacionService.markAsRead(tenantBNotification.getCodnot(), tenantAAuthentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createsNotificationsForPaymentConfirmationRejectionAndReceivedProof() {
        Context context = context("PAYMENT");
        Authentication ownerAuthentication = authentication(context.usuario());
        CuotaEntity confirmedQuota = cuota(context, LocalDate.of(2026, 9, 1), CuotaEstado.PENDIENTE);
        var confirmed = pagoService.create(confirmedQuota.getCodcuo(), paymentRequest(), null, ownerAuthentication);
        notificacionGeneracionService.generatePaymentConfirmed(confirmed.codpag());
        assertNotification(context.usuario(), NotificacionTipo.PAGO_CONFIRMADO, ReferenciaTipo.PAGO, confirmed.codpag());

        CuotaEntity rejectedQuota = cuota(context, LocalDate.of(2026, 10, 1), CuotaEstado.PENDIENTE);
        PagoEntity rejected = pendingPayment(context, rejectedQuota);
        pagoService.reject(rejected.getCodpag(), new PagoMotivoRequest("Comprobante inválido"), ownerAuthentication);
        notificacionGeneracionService.generatePaymentRejected(rejected.getCodpag());
        assertNotification(context.usuario(), NotificacionTipo.PAGO_RECHAZADO, ReferenciaTipo.PAGO, rejected.getCodpag());

        CuotaEntity proofQuota = cuota(context, LocalDate.of(2026, 11, 1), CuotaEstado.PENDIENTE);
        PagoEntity proofPayment = pendingPayment(context, proofQuota);
        PagoComprobanteEntity proof = new PagoComprobanteEntity();
        proof.setPago(proofPayment);
        proof.setRutaArchivo("comprobantes/" + proofPayment.getCodpag()
                + "/00000000-0000-0000-0000-000000000001.png");
        proof.setNombreArchivo("pago.png");
        proof.setTipoContenido("image/png");
        proof.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 12, 0));
        pagoComprobanteRepository.saveAndFlush(proof);
        notificacionGeneracionService.generateComprobanteReceived(proofPayment.getCodpag());
        assertThat(notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO, ReferenciaTipo.PAGO,
                proofPayment.getCodpag()).orElseThrow().getMensaje())
                .isEqualTo("Se registró un comprobante de pago para revisión.");
    }

    private void assertNotification(Usuario usuario, NotificacionTipo tipo, ReferenciaTipo referenciaTipo,
                                    Integer referenciaId) {
        assertThat(notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                usuario.getLogin(), tipo, referenciaTipo, referenciaId)).isPresent();
    }

    private PagoRequest paymentRequest() {
        return paymentRequest(new BigDecimal("100.00"));
    }

    private PagoRequest paymentRequest(BigDecimal amount) {
        return new PagoRequest(amount, MetodoPago.EFECTIVO, null, UUID.randomUUID());
    }

    private Context context(String prefix) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona propietaria = persona((prefix + "O" + token).substring(0, Math.min(20, prefix.length() + token.length() + 1)));
        Usuario usuario = new Usuario();
        String login = "not." + prefix.toLowerCase() + "." + token;
        usuario.setLogin(login.substring(0, Math.min(30, login.length())));
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(propietaria);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario = usuarioRepository.saveAndFlush(usuario);

        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Edificio " + prefix + token);
        propiedad.setTipo("EDIFICIO");
        propiedad.setDireccion("Calle de prueba");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(BigDecimal.ZERO);
        propiedad.setEstado((short) 1);
        propiedad = propiedadRepository.saveAndFlush(propiedad);

        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Departamento 2");
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("45.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(2);
        unidad.setPrecioBase(new BigDecimal("100.00"));
        unidad.setEstadoOperativo((short) 1);
        unidad = unidadRepository.saveAndFlush(unidad);

        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(persona(
                (prefix + "T" + token).substring(0, Math.min(20, prefix.length() + token.length() + 1)), 'I'));
        contrato.setFechaInicio(LocalDate.of(2026, 1, 1));
        contrato.setFechaFin(LocalDate.of(2027, 1, 1));
        contrato.setMontoMensual(new BigDecimal("100.00"));
        contrato.setMoneda("BOB");
        contrato.setGarantia(BigDecimal.ZERO);
        contrato.setEstado(ContratoEstado.VIGENTE);
        contrato.setFechaRegistro(LocalDateTime.now());
        contrato = contratoRepository.saveAndFlush(contrato);
        return new Context(usuario, contrato);
    }

    private PagoEntity pendingPayment(Context context, CuotaEntity cuota) {
        return pendingPayment(context, cuota, new BigDecimal("100.00"));
    }

    private PagoEntity pendingPayment(Context context, CuotaEntity cuota, BigDecimal amount) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setMonto(amount);
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(context.usuario());
        pago.setIdempotencyKey(UUID.randomUUID());
        return pagoRepository.saveAndFlush(pago);
    }

    private CuotaEntity cuota(Context context, LocalDate periodo, CuotaEstado estado) {
        return cuota(context, periodo, estado, new BigDecimal("100.00"));
    }

    private CuotaEntity cuota(Context context, LocalDate periodo, CuotaEstado estado, BigDecimal amount) {
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(context.contrato());
        cuota.setPeriodo(periodo);
        cuota.setFechaVencimiento(periodo);
        cuota.setMonto(amount);
        cuota.setEstado(estado);
        return cuotaRepository.saveAndFlush(cuota);
    }

    private Persona persona(String ci) {
        return persona(ci, 'A');
    }

    private Persona persona(String ci, Character tipoPersona) {
        String normalizedCi = ci.substring(0, Math.min(20, ci.length()));
        Persona persona = new Persona();
        persona.setCi(normalizedCi);
        persona.setNombre("Persona de notificación");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(normalizedCi.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona(tipoPersona);
        return personaRepository.saveAndFlush(persona);
    }

    private Authentication authentication(Usuario usuario) {
        return authentication(usuario, "ROLE_PROPIETARIO");
    }

    private Authentication authentication(Usuario usuario, String role) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private String periodDescription(LocalDate period) {
        return period.format(DateTimeFormatter.ofPattern("MMMM 'de' uuuu", Locale.forLanguageTag("es-BO")));
    }

    private NotificacionEntity notification(Usuario recipient, NotificacionTipo type, Integer referenceId) {
        return notification(recipient, type, referenceId, ReferenciaTipo.CUOTA);
    }

    private NotificacionEntity notification(Usuario recipient, NotificacionTipo type, Integer referenceId,
                                            ReferenciaTipo referenceType) {
        return notificacionRepository.saveAndFlush(notificacionMapper.toEntity(recipient, type, "Aviso de prueba",
                "NotificaciÃ³n de prueba.", referenceType, referenceId));
    }

    private Usuario user(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login.substring(0, Math.min(30, login.length())));
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(persona);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.saveAndFlush(usuario);
    }

    private record Context(Usuario usuario, ContratoEntity contrato) {
    }
}
