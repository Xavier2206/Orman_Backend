package com.orman.backend.payment.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.service.CuentaPagoService;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.payment.service.ReciboService;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class PaymentModuleIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoService pagoService;
    @Autowired private ReciboService reciboService;
    @Autowired private CuentaPagoService cuentaPagoService;
    @Autowired private CuotaService cuotaService;

    @Test
    void ownerRegistersCashAsConfirmedWithReceiptQuotaAndActors() {
        Context context = context("CASH", new BigDecimal("1500.00"));
        PagoResponse payment = pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("500.00"), MetodoPago.EFECTIVO, null, null), context.ownerAuth());

        assertThat(payment.estado()).isEqualTo("CONFIRMADO");
        assertThat(payment.origenRegistro()).isEqualTo("PROPIETARIA");
        assertThat(payment.registradoPor()).isEqualTo(context.owner().getLogin());
        assertThat(payment.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PARCIAL);
        assertThat(reciboService.getByPago(payment.codpag(), context.ownerAuth()).monto())
                .isEqualByComparingTo("500.00");

        var quota = cuotaService.listByContrato(context.contract().getCodcon(), context.ownerAuth()).getFirst();
        assertThat(quota.montoConfirmado()).isEqualByComparingTo("500.00");
        assertThat(quota.saldo()).isEqualByComparingTo("1000.00");
        assertThat(quota.montoPendienteRevision()).isZero();
    }

    @Test
    void ownerRegistersVerifiedQrDirectlyWithoutMandatoryProof() {
        Context context = context("OWNERQR", new BigDecimal("1000.00"));
        PagoResponse payment = pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("1000.00"), MetodoPago.QR, context.accountId(), null), context.ownerAuth());

        assertThat(payment.estado()).isEqualTo("CONFIRMADO");
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(reciboService.getByPago(payment.codpag(), context.ownerAuth()).metodo()).isEqualTo("QR");
    }

    @Test
    void rejectsDirectOwnerOverpayment() {
        Context context = context("OVERPAY", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("1000.01"), MetodoPago.EFECTIVO, null, null), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("saldo");
    }

    @Test
    void tenantQrRequiresProofAndTenantCashIsRejected() {
        Context context = context("TENANTRULES", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("100.00"), MetodoPago.QR, context.accountId(), null), context.tenantAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("comprobante");

        PagoResponse qrPending = pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("100.00"), MetodoPago.QR, context.accountId(), proof()), context.tenantAuth());
        assertThat(qrPending.estado()).isEqualTo("PENDIENTE_REVISION");
        assertThat(qrPending.origenRegistro()).isEqualTo("INQUILINO");

        assertThatThrownBy(() -> pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("100.00"), MetodoPago.EFECTIVO, null, proof()), context.tenantAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("efectivo");
    }

    @Test
    void tenantPresentsTransferPendingAndOwnerConfirmsWithReceiptAndTrace() {
        Context context = context("TRANSFER", new BigDecimal("1000.00"));
        PagoResponse pending = pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("1000.00"), MetodoPago.TRANSFERENCIA, context.accountId(), proof()),
                context.tenantAuth());

        assertThat(pending.estado()).isEqualTo("PENDIENTE_REVISION");
        assertThat(pending.origenRegistro()).isEqualTo("INQUILINO");
        assertThat(pending.registradoPor()).isEqualTo(context.tenant().getLogin());
        assertThat(pending.revisadoPor()).isNull();
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThatThrownBy(() -> reciboService.getByPago(pending.codpag(), context.ownerAuth()))
                .isInstanceOf(ResourceNotFoundException.class);

        PagoResponse confirmed = pagoService.confirm(pending.codpag(), context.ownerAuth());
        assertThat(confirmed.estado()).isEqualTo("CONFIRMADO");
        assertThat(confirmed.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(reciboService.getByPago(pending.codpag(), context.ownerAuth()).codpag()).isEqualTo(pending.codpag());
    }

    @Test
    void ownerRejectsTenantPaymentWithoutChangingQuotaOrCreatingReceipt() {
        Context context = context("REJECT", new BigDecimal("1000.00"));
        PagoResponse pending = presentQr(context, new BigDecimal("400.00"));
        PagoResponse rejected = pagoService.reject(pending.codpag(), new PagoMotivoRequest("No corresponde"),
                context.ownerAuth());

        assertThat(rejected.estado()).isEqualTo("RECHAZADO");
        assertThat(rejected.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
        assertThatThrownBy(() -> reciboService.getByPago(pending.codpag(), context.ownerAuth()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownerAnnulsOnlyPendingTenantRecordAndTracksReviewer() {
        Context context = context("ANNUL", new BigDecimal("1000.00"));
        PagoResponse pending = presentQr(context, new BigDecimal("200.00"));
        PagoResponse annulled = pagoService.annul(pending.codpag(), new PagoMotivoRequest("Duplicado"),
                context.ownerAuth());
        assertThat(annulled.estado()).isEqualTo("ANULADO");
        assertThat(annulled.revisadoPor()).isEqualTo(context.owner().getLogin());
        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PENDIENTE);
    }

    @Test
    void twoConfirmedPartialPaymentsCompleteQuotaAndCreateTwoReceipts() {
        Context context = context("PARTIAL", new BigDecimal("1500.00"));
        PagoResponse first = pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("500.00"), MetodoPago.EFECTIVO, null, null), context.ownerAuth());
        PagoResponse second = presentQr(context, new BigDecimal("1000.00"));
        pagoService.confirm(second.codpag(), context.ownerAuth());

        assertThat(cuota(context).getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(reciboService.getByPago(first.codpag(), context.ownerAuth()).codrec()).isNotNull();
        assertThat(reciboService.getByPago(second.codpag(), context.ownerAuth()).codrec()).isNotNull();
        assertThat(reciboService.getByPago(first.codpag(), context.ownerAuth()).codrec())
                .isNotEqualTo(reciboService.getByPago(second.codpag(), context.ownerAuth()).codrec());
    }

    @Test
    void annulledQuotaNeverAcceptsPayment() {
        Context context = context("ANNULLEDQUOTA", new BigDecimal("1000.00"));
        context.cuota().setEstado(CuotaEstado.ANULADA);
        cuotaRepository.saveAndFlush(context.cuota());
        assertThatThrownBy(() -> pagoService.create(context.cuota().getCodcuo(),
                request(new BigDecimal("100.00"), MetodoPago.EFECTIVO, null, null), context.ownerAuth()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("no admite");
    }

    private PagoResponse presentQr(Context context, BigDecimal amount) {
        return pagoService.create(context.cuota().getCodcuo(),
                request(amount, MetodoPago.QR, context.accountId(), proof()), context.tenantAuth());
    }

    private PagoRequest request(BigDecimal amount, MetodoPago method, Integer accountId,
                                PagoComprobanteRequest proof) {
        return new PagoRequest(amount, method, accountId, "REF-" + UUID.randomUUID(), LocalDateTime.now(),
                UUID.randomUUID(), proof);
    }

    private PagoComprobanteRequest proof() {
        return new PagoComprobanteRequest("https://example.test/comprobante.pdf", "comprobante.pdf",
                "application/pdf", 0);
    }

    private CuotaEntity cuota(Context context) {
        return cuotaRepository.findByCodcuo(context.cuota().getCodcuo()).orElseThrow();
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
        contract.setFechaFin(LocalDate.of(2027, 1, 1));
        contract.setMontoMensual(amount);
        contract.setMoneda("BOB");
        contract.setGarantia(BigDecimal.ZERO);
        contract.setEstado(ContratoEstado.VIGENTE);
        contract.setFechaRegistro(LocalDateTime.now());
        contract = contratoRepository.saveAndFlush(contract);

        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(LocalDate.of(2026, 9, 1));
        quota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        quota.setMonto(amount);
        quota.setEstado(CuotaEstado.PENDIENTE);
        quota = cuotaRepository.saveAndFlush(quota);

        Authentication ownerAuth = authentication(owner, "ROLE_PROPIETARIO");
        Integer accountId = cuentaPagoService.create(new CuentaPagoRequest("Banco", token, "Carmen", null,
                null, 0, "1"), ownerAuth).codcta();
        return new Context(owner, tenant, ownerAuth, authentication(tenant, "ROLE_INQUILINO"), contract, quota,
                accountId);
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
        user.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.saveAndFlush(user);
    }

    private Authentication authentication(Usuario user, String role) {
        return new TestingAuthenticationToken(new AuthenticatedUser(user.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private record Context(Usuario owner, Usuario tenant, Authentication ownerAuth, Authentication tenantAuth,
                           ContratoEntity contract, CuotaEntity cuota, Integer accountId) {
    }
}
