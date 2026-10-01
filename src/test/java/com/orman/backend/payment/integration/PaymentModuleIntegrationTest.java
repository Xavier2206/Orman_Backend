package com.orman.backend.payment.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.request.QrCobroRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.dto.response.QrCobroResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.payment.service.PagoComprobanteService;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.payment.service.QrCobroService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PaymentModuleIntegrationTest.FixedClockConfiguration.class)
@Transactional
@Rollback
class PaymentModuleIntegrationTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PagoComprobanteRepository comprobanteRepository;
    @Autowired private PaymentImageStorageService imageStorageService;
    @Autowired private PagoService pagoService;
    @Autowired private PagoComprobanteService comprobanteService;
    @Autowired private QrCobroService qrCobroService;
    @Autowired private Clock clock;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String rolledBackProofPath;
    private Integer rolledBackProofPaymentId;

    @Test
    void postgresSessionsUseTheBolivianZoneForTimestampDefaults() {
        String databaseZone = jdbcTemplate.queryForObject("SELECT current_setting('TimeZone')", String.class);

        assertThat(databaseZone).isEqualTo("America/La_Paz");
    }

    @Test
    void ownerCashUsesLaPazTimeConfirmsAndUpdatesPartialBalance() {
        Context context = context("CASH", new BigDecimal("1500.00"));
        PagoResponse payment = pagoService.create(context.quota().getCodcuo(), request("500.00", MetodoPago.EFECTIVO,
                null), null, context.ownerAuth());

        assertThat(payment.estado()).isEqualTo("CONFIRMADO");
        assertThat(payment.origenRegistro()).isEqualTo("PROPIETARIA");
        assertThat(payment.codqr()).isNull();
        assertThat(payment.fechaPago()).isEqualTo(nowLaPaz().atZone(LA_PAZ).toOffsetDateTime());
        assertThat(payment.fechaRegistro()).isEqualTo(nowLaPaz().atZone(LA_PAZ).toOffsetDateTime());
        assertThat(payment.fechaRevision()).isEqualTo(nowLaPaz().atZone(LA_PAZ).toOffsetDateTime());
        PagoEntity storedPayment = pagoRepository.findById(payment.codpag()).orElseThrow();
        assertThat(storedPayment.getFechaRegistro()).isEqualTo(LocalDateTime.of(2026, 9, 25, 12, 20));
        assertThat(storedPayment.getFechaRevision()).isEqualTo(LocalDateTime.of(2026, 9, 25, 12, 20));
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);
        assertThat(comprobanteRepository.existsByPagoCodpag(payment.codpag())).isFalse();
    }

    @Test
    void ownerQrResolvesCurrentQrAutomaticallyAndConfirmsWithoutProof() throws Exception {
        Context context = context("OWNERQR", new BigDecimal("1000.00"));
        QrCobroResponse qr = createQr(context, "owner-qr.png");
        LocalDateTime occurredAt = nowLaPaz().minusMinutes(15);

        PagoResponse pastPayment = pagoService.create(context.quota().getCodcuo(),
                request("400.00", MetodoPago.QR, occurredAt), null, context.ownerAuth());

        assertThat(pastPayment.codqr()).isEqualTo(qr.codqr());
        assertThat(pastPayment.fechaPago()).isEqualTo(occurredAt.atZone(LA_PAZ).toOffsetDateTime());
        assertThat(pastPayment.estado()).isEqualTo("CONFIRMADO");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);

        PagoResponse currentPayment = pagoService.create(context.quota().getCodcuo(),
                request("600.00", MetodoPago.QR, nowLaPaz()), null, context.ownerAuth());
        assertThat(currentPayment.fechaPago()).isEqualTo(nowLaPaz().atZone(LA_PAZ).toOffsetDateTime());
        assertThat(currentPayment.estado()).isEqualTo("CONFIRMADO");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(comprobanteRepository.existsByPagoCodpag(pastPayment.codpag())).isFalse();
        assertThat(comprobanteRepository.existsByPagoCodpag(currentPayment.codpag())).isFalse();
    }

    @Test
    void tenantQrRequiresProofStaysPendingAndOnlyConfirmationUpdatesQuota() throws Exception {
        Context context = context("TENANTQR", new BigDecimal("1000.00"));
        QrCobroResponse qr = createQr(context, "tenant-qr.png");
        LocalDateTime occurredAt = nowLaPaz().minusMinutes(10);
        PagoRequest request = request("1000.00", MetodoPago.QR, occurredAt);

        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(), request, null, context.tenantAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("comprobante");

        PagoResponse pending = pagoService.create(context.quota().getCodcuo(), request, png("proof.png"),
                context.tenantAuth());
        assertThat(pending.estado()).isEqualTo("PENDIENTE_REVISION");
        assertThat(pending.origenRegistro()).isEqualTo("INQUILINO");
        assertThat(pending.codqr()).isEqualTo(qr.codqr());
        assertThat(comprobanteRepository.existsByPagoCodpag(pending.codpag())).isTrue();
        var storedProof = comprobanteRepository.findByPagoCodpag(pending.codpag()).orElseThrow();
        assertThat(storedProof.getFechaRegistro()).isEqualTo(nowLaPaz());
        rolledBackProofPath = storedProof.getRutaArchivo();
        rolledBackProofPaymentId = pending.codpag();
        assertThat(comprobanteService.getMetadata(pending.codpag(), context.tenantAuth()).tipoContenido())
                .isEqualTo("image/png");
        assertThat(comprobanteService.getImage(pending.codpag(), context.ownerAuth()).tipoContenido())
                .isEqualTo("image/png");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        Context other = context("THIRDPARTY", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> comprobanteService.getMetadata(pending.codpag(), other.tenantAuth()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        PagoResponse confirmed = pagoService.confirm(pending.codpag(), context.ownerAuth());
        assertThat(confirmed.estado()).isEqualTo("CONFIRMADO");
        assertThat(confirmed.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
    }

    @Test
    void ownerRejectsTenantQrWithoutReducingConfirmedBalanceAndAnnulRemainsDistinct() throws Exception {
        Context context = context("REJECT", new BigDecimal("1000.00"));
        createQr(context, "reject-qr.png");
        PagoResponse rejectedPending = presentQr(context, "400.00");
        PagoResponse rejected = pagoService.reject(rejectedPending.codpag(),
                new PagoMotivoRequest("No corresponde"), context.ownerAuth());
        assertThat(rejected.estado()).isEqualTo("RECHAZADO");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);

        PagoResponse annulPending = presentQr(context, "200.00");
        PagoResponse annulled = pagoService.annul(annulPending.codpag(),
                new PagoMotivoRequest("Duplicado"), context.ownerAuth());
        assertThat(annulled.estado()).isEqualTo("ANULADO");
        assertThat(annulled.motivoAnulacion()).isEqualTo("Duplicado");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
    }

    @Test
    void ownerCanAnnulConfirmedCashAndReturnPartialQuotaToPending() {
        Context context = context("ANNULCASH", new BigDecimal("2500.00"));
        PagoResponse confirmed = pagoService.create(context.quota().getCodcuo(),
                request("500.00", MetodoPago.EFECTIVO, null), null, context.ownerAuth());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);

        PagoResponse annulled = pagoService.annul(confirmed.codpag(),
                new PagoMotivoRequest("  Se registró por error.  "), context.ownerAuth());

        assertThat(annulled.estado()).isEqualTo("ANULADO");
        assertThat(annulled.motivoAnulacion()).isEqualTo("Se registró por error.");
        assertThat(annulled.motivoRechazo()).isNull();
        assertThat(annulled.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(annulled.fechaRevision()).isEqualTo(nowLaPaz().atZone(LA_PAZ).toOffsetDateTime());
        assertThat(pagoRepository.sumConfirmedMontoByCuota(context.quota().getCodcuo()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThat(pagoService.listByCuota(context.quota().getCodcuo(), context.ownerAuth()))
                .extracting(PagoResponse::estado).containsExactly("ANULADO");
    }

    @Test
    void ownerCanAnnulConfirmedQrAndKeepsQrAndProof() throws Exception {
        Context context = context("ANNULQR", new BigDecimal("1000.00"));
        QrCobroResponse qr = createQr(context, "annul-owner-qr.png");
        PagoResponse confirmed = pagoService.create(context.quota().getCodcuo(),
                request("400.00", MetodoPago.QR, nowLaPaz().minusMinutes(5)),
                png("annul-owner-proof.png"), context.ownerAuth());
        var proof = comprobanteRepository.findByPagoCodpag(confirmed.codpag()).orElseThrow();
        rolledBackProofPath = proof.getRutaArchivo();
        rolledBackProofPaymentId = confirmed.codpag();

        PagoResponse annulled = pagoService.annul(confirmed.codpag(),
                new PagoMotivoRequest("Pago QR duplicado"), context.ownerAuth());

        assertThat(annulled.estado()).isEqualTo("ANULADO");
        assertThat(annulled.codqr()).isEqualTo(qr.codqr());
        assertThat(pagoRepository.sumConfirmedMontoByCuota(context.quota().getCodcuo()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThat(comprobanteRepository.existsByPagoCodpag(confirmed.codpag())).isTrue();
        assertThat(comprobanteService.getImage(confirmed.codpag(), context.ownerAuth()).tamano()).isPositive();
    }

    @Test
    void annulConfirmedPaymentRecalculatesRemainingPartialTotal() {
        Context context = context("ANNULMULTI", new BigDecimal("2500.00"));
        PagoResponse paymentA = pagoService.create(context.quota().getCodcuo(),
                request("500.00", MetodoPago.EFECTIVO, null), null, context.ownerAuth());
        pagoService.create(context.quota().getCodcuo(), request("700.00", MetodoPago.EFECTIVO, null),
                null, context.ownerAuth());

        pagoService.annul(paymentA.codpag(), new PagoMotivoRequest("Corrección"), context.ownerAuth());

        assertThat(pagoRepository.sumConfirmedMontoByCuota(context.quota().getCodcuo()))
                .isEqualByComparingTo("700.00");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);
    }

    @Test
    void annullingPaymentThatCompletedQuotaReturnsItToPartialAndThenPending() {
        Context partialContext = context("ANNULFULLMULTI", new BigDecimal("2500.00"));
        pagoService.create(partialContext.quota().getCodcuo(), request("1000.00", MetodoPago.EFECTIVO, null),
                null, partialContext.ownerAuth());
        PagoResponse completingPayment = pagoService.create(partialContext.quota().getCodcuo(),
                request("1500.00", MetodoPago.EFECTIVO, null), null, partialContext.ownerAuth());
        assertThat(cuota(partialContext).getEstado()).isEqualTo(CuotaEstado.PAGADA);

        pagoService.annul(completingPayment.codpag(), new PagoMotivoRequest("Corrección"),
                partialContext.ownerAuth());

        assertThat(pagoRepository.sumConfirmedMontoByCuota(partialContext.quota().getCodcuo()))
                .isEqualByComparingTo("1000.00");
        assertThat(cuota(partialContext).getEstado()).isEqualTo(CuotaEstado.PARCIAL);

        Context totalContext = context("ANNULTOTAL", new BigDecimal("2500.00"));
        PagoResponse totalPayment = pagoService.create(totalContext.quota().getCodcuo(),
                request("2500.00", MetodoPago.EFECTIVO, null), null, totalContext.ownerAuth());
        pagoService.annul(totalPayment.codpag(), new PagoMotivoRequest("Registro equivocado"),
                totalContext.ownerAuth());

        assertThat(pagoRepository.sumConfirmedMontoByCuota(totalContext.quota().getCodcuo()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuota(totalContext).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
    }

    @Test
    void confirmedTenantPaymentCannotBeAnnulledAndKeepsProofAndQr() throws Exception {
        Context context = context("ANNULTENANT", new BigDecimal("1000.00"));
        QrCobroResponse qr = createQr(context, "annul-tenant-qr.png");
        PagoResponse pending = presentQr(context, "1000.00");
        var proof = comprobanteRepository.findByPagoCodpag(pending.codpag()).orElseThrow();
        rolledBackProofPath = proof.getRutaArchivo();
        rolledBackProofPaymentId = pending.codpag();
        PagoResponse confirmed = pagoService.confirm(pending.codpag(), context.ownerAuth());

        assertThatThrownBy(() -> pagoService.annul(confirmed.codpag(),
                new PagoMotivoRequest("No corresponde"), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Solo pueden anularse pagos confirmados registrados por la propietaria.");

        PagoResponse unchanged = pagoService.get(confirmed.codpag(), context.ownerAuth());
        assertThat(unchanged.estado()).isEqualTo("CONFIRMADO");
        assertThat(unchanged.origenRegistro()).isEqualTo("INQUILINO");
        assertThat(unchanged.codqr()).isEqualTo(qr.codqr());
        assertThat(pagoRepository.sumConfirmedMontoByCuota(context.quota().getCodcuo()))
                .isEqualByComparingTo("1000.00");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(comprobanteRepository.existsByPagoCodpag(confirmed.codpag())).isTrue();
        assertThat(comprobanteService.getImage(confirmed.codpag(), context.ownerAuth()).tamano()).isPositive();
    }

    @Test
    void annullingConfirmedPaymentDoesNotChangeOtherPendingPayment() throws Exception {
        Context context = context("ANNULPENDING", new BigDecimal("2500.00"));
        PagoResponse confirmedOwnerPayment = pagoService.create(context.quota().getCodcuo(),
                request("2000.00", MetodoPago.EFECTIVO, null), null, context.ownerAuth());
        createQr(context, "annul-pending-qr.png");
        PagoResponse pendingTenantPayment = presentQr(context, "500.00");
        var proof = comprobanteRepository.findByPagoCodpag(pendingTenantPayment.codpag()).orElseThrow();
        rolledBackProofPath = proof.getRutaArchivo();
        rolledBackProofPaymentId = pendingTenantPayment.codpag();

        pagoService.annul(confirmedOwnerPayment.codpag(), new PagoMotivoRequest("Corrección"), context.ownerAuth());

        assertThat(pagoService.get(pendingTenantPayment.codpag(), context.ownerAuth()).estado())
                .isEqualTo("PENDIENTE_REVISION");
        assertThat(pagoRepository.sumConfirmedMontoByCuota(context.quota().getCodcuo()))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cuotaRepository.sumPaymentAmountByState(context.quota().getCodcuo(), "PENDIENTE_REVISION"))
                .isEqualByComparingTo("500.00");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
    }

    @Test
    void rejectedAndAlreadyAnnulledPaymentsCannotBeAnnulledAgain() throws Exception {
        Context context = context("ANNULTERMINAL", new BigDecimal("1000.00"));
        createQr(context, "annul-terminal-qr.png");
        PagoResponse rejectedPending = presentQr(context, "100.00");
        rememberProof(rejectedPending);
        pagoService.reject(rejectedPending.codpag(), new PagoMotivoRequest("No corresponde"), context.ownerAuth());
        assertThatThrownBy(() -> pagoService.annul(rejectedPending.codpag(),
                new PagoMotivoRequest("Intento inválido"), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void alreadyAnnulledPaymentCannotBeAnnulledAgain() throws Exception {
        Context context = context("ANNULTWICE", new BigDecimal("1000.00"));
        createQr(context, "annul-twice-qr.png");
        PagoResponse pending = presentQr(context, "100.00");
        rememberProof(pending);
        PagoResponse annulled = pagoService.annul(pending.codpag(), new PagoMotivoRequest("Duplicado"),
                context.ownerAuth());
        assertThat(annulled.estado()).isEqualTo("ANULADO");
        assertThatThrownBy(() -> pagoService.annul(pending.codpag(),
                new PagoMotivoRequest("Segundo intento"), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(pagoService.get(pending.codpag(), context.ownerAuth()).estado()).isEqualTo("ANULADO");
    }

    @Test
    void onlyOwningPropertyOwnerCanAnnulConfirmedPayment() {
        Context context = context("ANNULOWNER", new BigDecimal("1000.00"));
        PagoResponse confirmed = pagoService.create(context.quota().getCodcuo(),
                request("500.00", MetodoPago.EFECTIVO, null), null, context.ownerAuth());
        Context otherOwner = context("ANNALOTHER", new BigDecimal("1000.00"));

        assertThatThrownBy(() -> pagoService.annul(confirmed.codpag(),
                new PagoMotivoRequest("No es mi cuota"), otherOwner.ownerAuth()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(pagoService.get(confirmed.codpag(), context.ownerAuth()).estado()).isEqualTo("CONFIRMADO");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);
    }

    @Test
    void confirmedPaymentCanBeAnnulledForProgrammedContractButNotClosedOrAnnulledRecords() {
        Context programmed = context("ANNULPROGRAM", new BigDecimal("1000.00"));
        PagoResponse programmedPayment = pagoService.create(programmed.quota().getCodcuo(),
                request("100.00", MetodoPago.EFECTIVO, null), null, programmed.ownerAuth());
        programmed.contract().setEstado(ContratoEstado.PROGRAMADO);
        contratoRepository.saveAndFlush(programmed.contract());
        assertThat(pagoService.annul(programmedPayment.codpag(), new PagoMotivoRequest("Corrección"),
                programmed.ownerAuth()).estado()).isEqualTo("ANULADO");

        Context finalized = context("ANNULFINAL", new BigDecimal("1000.00"));
        PagoResponse finalizedPayment = pagoService.create(finalized.quota().getCodcuo(),
                request("100.00", MetodoPago.EFECTIVO, null), null, finalized.ownerAuth());
        finalized.contract().setEstado(ContratoEstado.FINALIZADO);
        contratoRepository.saveAndFlush(finalized.contract());
        assertThatThrownBy(() -> pagoService.annul(finalizedPayment.codpag(),
                new PagoMotivoRequest("Corrección"), finalized.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("contrato finalizado");

        Context rescinded = context("ANNULRESCIND", new BigDecimal("1000.00"));
        PagoResponse rescindedPayment = pagoService.create(rescinded.quota().getCodcuo(),
                request("100.00", MetodoPago.EFECTIVO, null), null, rescinded.ownerAuth());
        rescinded.contract().setEstado(ContratoEstado.RESCINDIDO);
        rescinded.contract().setFechaRescision(LocalDate.of(2027, 1, 1));
        rescinded.contract().setMotivoRescision("Prueba");
        contratoRepository.saveAndFlush(rescinded.contract());
        assertThatThrownBy(() -> pagoService.annul(rescindedPayment.codpag(),
                new PagoMotivoRequest("Corrección"), rescinded.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("contrato rescindido");

        Context annulledQuota = context("ANNULQUOTA", new BigDecimal("1000.00"));
        PagoResponse annulledQuotaPayment = pagoService.create(annulledQuota.quota().getCodcuo(),
                request("100.00", MetodoPago.EFECTIVO, null), null, annulledQuota.ownerAuth());
        annulledQuota.quota().setEstado(CuotaEstado.ANULADA);
        cuotaRepository.saveAndFlush(annulledQuota.quota());
        assertThatThrownBy(() -> pagoService.annul(annulledQuotaPayment.codpag(),
                new PagoMotivoRequest("Corrección"), annulledQuota.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("cuota anulada");
    }

    @Test
    void qrRequiresDateRejectsFutureAndDateMustFallWithinCurrentQrValidity() throws Exception {
        Context context = context("DATES", new BigDecimal("1000.00"));
        createQr(context, "dates-qr.png");
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("100.00", MetodoPago.QR, null), null, context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("fechaPago");
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("100.00", MetodoPago.QR, nowLaPaz().plusSeconds(1)), null, context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("futuro");
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("100.00", MetodoPago.QR, LocalDateTime.of(2024, 1, 1, 12, 0)), null,
                context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("vigencia");
    }

    @Test
    void qrIsAccessibleByQuotaTenantAndCannotBeManagedByAnotherOwner() throws Exception {
        Context context = context("ACCESS", new BigDecimal("500.00"));
        QrCobroResponse qr = createQr(context, "access-qr.png");
        assertThat(qrCobroService.currentForQuota(context.quota().getCodcuo(), context.tenantAuth()).codqr())
                .isEqualTo(qr.codqr());
        assertThat(qrCobroService.imageForQuota(context.quota().getCodcuo(), context.tenantAuth()).tipoContenido())
                .isEqualTo("image/png");
        assertThat(qr.toString()).doesNotContain("qr-cobro/", "C:\\");
        Context other = context("OTHER", new BigDecimal("500.00"));
        assertThatThrownBy(() -> qrCobroService.currentForQuota(context.quota().getCodcuo(), other.tenantAuth()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> qrCobroService.changeState(qr.codqr(),
                new com.orman.backend.payment.dto.request.QrCobroEstadoRequest(
                        com.orman.backend.payment.entity.QrCobroEstado.INACTIVO), other.ownerAuth()))
                .isInstanceOf(com.orman.backend.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void activeQrValidityCannotOverlap() throws Exception {
        Context context = context("OVERLAP", new BigDecimal("500.00"));
        createQr(context, "first-qr.png");
        assertThatThrownBy(() -> qrCobroService.create(
                new QrCobroRequest(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1)),
                png("second-qr.png"), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("superpone");
    }

    @Test
    void rejectsQrWithInvertedValidityDates() {
        Context context = context("BADRANGE", new BigDecimal("500.00"));
        assertThatThrownBy(() -> qrCobroService.create(
                new QrCobroRequest(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 12, 31)),
                png("invalid-qr.png"), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("fechaInicio");
    }

    @Test
    void partialPaymentsCompleteQuotaAndIdempotencyAndOverpaymentRemainEnforced() {
        Context context = context("PARTIAL", new BigDecimal("1000.00"));
        PagoRequest firstRequest = request("400.00", MetodoPago.EFECTIVO, null);
        PagoResponse first = pagoService.create(context.quota().getCodcuo(), firstRequest, null, context.ownerAuth());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(), firstRequest, null,
                context.ownerAuth())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("600.01", MetodoPago.EFECTIVO, null), null, context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("saldo");
        pagoService.create(context.quota().getCodcuo(), request("600.00", MetodoPago.EFECTIVO, null), null,
                context.ownerAuth());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(pagoRepository.findById(first.codpag())).isPresent();
    }

    @Test
    void tenantCannotPresentCashAndQrWithoutCurrentQrIsRejected() {
        Context context = context("NOQR", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("100.00", MetodoPago.QR, nowLaPaz()), png("proof.png"), context.tenantAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("QR de cobro vigente");
        assertThatThrownBy(() -> pagoService.create(context.quota().getCodcuo(),
                request("100.00", MetodoPago.EFECTIVO, null), null, context.tenantAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("solo puede");
    }

    private PagoResponse presentQr(Context context, String amount) {
        return pagoService.create(context.quota().getCodcuo(), request(amount, MetodoPago.QR, nowLaPaz().minusMinutes(1)),
                png("proof.png"), context.tenantAuth());
    }

    private void rememberProof(PagoResponse payment) {
        var proof = comprobanteRepository.findByPagoCodpag(payment.codpag()).orElseThrow();
        rolledBackProofPath = proof.getRutaArchivo();
        rolledBackProofPaymentId = payment.codpag();
    }

    private QrCobroResponse createQr(Context context, String fileName) throws Exception {
        return qrCobroService.create(new QrCobroRequest(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31)),
                png(fileName), context.ownerAuth());
    }

    private PagoRequest request(String amount, MetodoPago method, LocalDateTime fechaPago) {
        return new PagoRequest(new BigDecimal(amount), method, fechaPago, UUID.randomUUID());
    }

    private MockMultipartFile png(String filename) {
        try {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, 128, 128);
                graphics.setColor(Color.BLACK);
                for (int y = 8; y < 120; y += 8) {
                    for (int x = 8; x < 120; x += 8) {
                        if (((x + y) / 8) % 2 == 0) graphics.fillRect(x, y, 8, 8);
                    }
                }
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return new MockMultipartFile("imagen", filename, "image/png", bytes.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private LocalDateTime nowLaPaz() {
        return LocalDateTime.ofInstant(clock.instant(), LA_PAZ);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private CuotaEntity cuota(Context context) {
        return cuotaRepository.findByCodcuo(context.quota().getCodcuo()).orElseThrow();
    }

    private Context context(String prefix, BigDecimal amount) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona ownerPerson = person(prefix + "O" + token);
        Usuario owner = user("pay.owner." + token, ownerPerson);
        Persona tenantPerson = person(prefix + "T" + token);
        Usuario tenant = user("pay.tenant." + token, tenantPerson);

        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Propiedad " + prefix + token);
        property.setTipo("CASA");
        property.setDireccion("Calle de prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(ownerPerson);
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado((short) 1);
        property = propiedadRepository.saveAndFlush(property);

        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + prefix + token);
        unit.setTipoUnidad("DEPARTAMENTO");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(amount);
        unit.setEstadoOperativo((short) 1);
        unit = unidadRepository.saveAndFlush(unit);

        ContratoEntity contract = new ContratoEntity();
        contract.setUnidad(unit);
        contract.setInquilino(tenantPerson);
        contract.setFechaInicio(LocalDate.of(2026, 1, 1));
        contract.setFechaFin(LocalDate.of(2028, 1, 1));
        contract.setMontoMensual(amount);
        contract.setMoneda("BOB");
        contract.setGarantia(BigDecimal.ZERO);
        contract.setEstado(ContratoEstado.VIGENTE);
        contract.setFechaRegistro(nowUtc());
        contract = contratoRepository.saveAndFlush(contract);

        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(LocalDate.of(2026, 9, 1));
        quota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        quota.setMonto(amount);
        quota.setEstado(CuotaEstado.PENDIENTE);
        quota = cuotaRepository.saveAndFlush(quota);
        return new Context(owner, tenant, authentication(owner, "ROLE_PROPIETARIO"),
                authentication(tenant, "ROLE_INQUILINO"), contract, quota);
    }

    private Persona person(String ci) {
        String normalized = ci.substring(0, Math.min(20, ci.length()));
        Persona person = new Persona();
        person.setCi(normalized);
        person.setNombre("Persona de pagos");
        person.setGenero('F');
        person.setEstado((short) 1);
        person.setCorreo(normalized.toLowerCase() + "@example.test");
        person.setTelefono("70000000");
        person.setTipoPersona('A');
        return personaRepository.saveAndFlush(person);
    }

    private Usuario user(String login, Persona person) {
        Usuario user = new Usuario();
        user.setLogin(login.substring(0, Math.min(30, login.length())));
        user.setPasswd("hash-no-expuesto");
        user.setEstado((short) 1);
        user.setPersona(person);
        user.setFechaCreacion(nowUtc());
        return usuarioRepository.saveAndFlush(user);
    }

    private Authentication authentication(Usuario user, String role) {
        return new TestingAuthenticationToken(new AuthenticatedUser(user.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    @AfterTransaction
    void rolledBackTransactionRemovesStoredProofFile() {
        if (rolledBackProofPath != null) {
            assertThatThrownBy(() -> imageStorageService.loadProof(rolledBackProofPath, rolledBackProofPaymentId))
                    .isInstanceOf(com.orman.backend.common.exception.ResourceNotFoundException.class);
            rolledBackProofPath = null;
            rolledBackProofPaymentId = null;
        }
    }

    private record Context(Usuario owner, Usuario tenant, Authentication ownerAuth, Authentication tenantAuth,
                           ContratoEntity contract, CuotaEntity quota) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock paymentIntegrationTestClock() {
            return Clock.fixed(Instant.parse("2026-09-25T16:20:00Z"), ZoneOffset.UTC);
        }
    }
}
