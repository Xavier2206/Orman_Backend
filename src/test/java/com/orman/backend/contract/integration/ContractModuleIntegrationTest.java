package com.orman.backend.contract.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.impl.ContratoActivacionScheduler;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.repository.PagoRepository;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
class ContractModuleIntegrationTest {

    private static final LocalDate MES_ACTUAL = LocalDate.now(ZoneId.of("America/La_Paz")).withDayOfMonth(1);

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private ContratoService contratoService;
    @Autowired private ContratoActivacionScheduler contratoActivacionScheduler;

    @Test
    void createsCurrentContractAsActiveAndFutureContractAsScheduledWithQuotas() {
        Context current = context("CURRENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse vigente = create(current, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        assertThat(vigente.estado()).isEqualTo("VIGENTE");
        assertThat(vigente.moneda()).isEqualTo("BOB");
        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(vigente.codcon())).hasSize(2);

        Context future = context("FUTURE", (short) 1, (short) 1, (short) 1);
        ContratoResponse programado = create(future, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(3));
        assertThat(programado.estado()).isEqualTo("PROGRAMADO");
        assertThat(cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(programado.codcon())).hasSize(2);
    }

    @Test
    void filtersContractsByPropertyUnitAndStateWithoutCrossingOwnerBoundary() {
        Context propertyA = context("FILTERA", (short) 1, (short) 1, (short) 1);
        ContratoResponse propertyACurrent = create(propertyA, MES_ACTUAL, MES_ACTUAL.plusMonths(1));
        ContratoResponse propertyAFuture = create(propertyA, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(2));

        Context propertyB = context("FILTERB", (short) 1, (short) 1, (short) 1);
        create(propertyB, MES_ACTUAL, MES_ACTUAL.plusMonths(1));

        Integer codpropA = propertyA.unidad().getPropiedad().getCodprop();
        Integer codpropB = propertyB.unidad().getPropiedad().getCodprop();

        assertThat(contratoService.list(codpropA, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(codpropA, propertyA.unidad().getCoduni(), null, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(codpropA, propertyA.unidad().getCoduni(), ContratoEstado.VIGENTE, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactly(propertyACurrent.codcon());
        assertThat(contratoService.list(codpropA, propertyB.unidad().getCoduni(), null, page(),
                propertyA.authentication()).content()).isEmpty();
        assertThat(contratoService.list(codpropA, null, ContratoEstado.VIGENTE, page(),
                propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactly(propertyACurrent.codcon());
        assertThat(contratoService.list(codpropB, null, null, page(), propertyA.authentication()).content())
                .isEmpty();
        assertThat(contratoService.list(null, null, null, page(), propertyA.authentication()).content())
                .extracting(ContratoResponse::codcon)
                .containsExactlyInAnyOrder(propertyACurrent.codcon(), propertyAFuture.codcon());
        assertThat(contratoService.list(codpropA, null, null, page(), propertyB.authentication()).content())
                .isEmpty();
    }

    @Test
    void scheduledContractBecomesActiveOnItsStartDateIdempotently() {
        Context context = context("SCHEDULE", (short) 1, (short) 1, (short) 1);
        LocalDate start = MES_ACTUAL.plusMonths(1);
        ContratoResponse contract = create(context, start, start.plusMonths(2));

        assertThat(contratoActivacionScheduler.activateFor(start)).isEqualTo(1);
        assertThat(contratoActivacionScheduler.activateFor(start)).isZero();
        assertThat(contratoService.get(contract.codcon(), context.authentication()).estado()).isEqualTo("VIGENTE");
    }

    @Test
    void acceptsContiguousContractsAndRejectsOverlappingIntervals() {
        Context context = context("OVERLAP", (short) 1, (short) 1, (short) 1);
        create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        create(context, MES_ACTUAL.plusMonths(2), MES_ACTUAL.plusMonths(4));

        assertThatThrownBy(() -> create(context, MES_ACTUAL.plusMonths(1), MES_ACTUAL.plusMonths(3)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    void revalidatesPropertyUnitTenantAndMoneyWhenRegistering() {
        Context inactiveProperty = context("PROPERTY", (short) 0, (short) 1, (short) 1);
        assertThatThrownBy(() -> create(inactiveProperty, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Propiedad");

        Context inactiveUnit = context("UNIT", (short) 1, (short) 0, (short) 1);
        assertThatThrownBy(() -> create(inactiveUnit, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Unidad");

        Context inactiveTenant = context("TENANT", (short) 1, (short) 1, (short) 0);
        assertThatThrownBy(() -> create(inactiveTenant, MES_ACTUAL, MES_ACTUAL.plusMonths(1)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inquilina");

        Context money = context("MONEY", (short) 1, (short) 1, (short) 1);
        assertThatThrownBy(() -> contratoService.create(money.unidad().getCoduni(),
                request(money.inquilino().getCodper(), MES_ACTUAL, MES_ACTUAL.plusMonths(1), BigDecimal.ZERO),
                money.authentication())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("mayor a cero");
    }

    @Test
    void rejectsFinishingBeforeContractEnd() {
        Context context = context("EARLY", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL, MES_ACTUAL.plusMonths(2));
        assertThatThrownBy(() -> contratoService.finish(contract.codcon(), context.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("antes");
    }

    @Test
    void rejectsFinishingWithPendingOrPartialQuota() {
        Context pendingContext = context("FINPENDING", (short) 1, (short) 1, (short) 1);
        ContratoResponse pending = create(pendingContext, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        assertThatThrownBy(() -> contratoService.finish(pending.codcon(), pendingContext.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pagadas");

        Context partialContext = context("FINPARTIAL", (short) 1, (short) 1, (short) 1);
        ContratoResponse partial = create(partialContext, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        CuotaEntity cuota = cuotas(partial).getFirst();
        cuota.setEstado(CuotaEstado.PARCIAL);
        cuotaRepository.saveAndFlush(cuota);
        assertThatThrownBy(() -> contratoService.finish(partial.codcon(), partialContext.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pagadas");
    }

    @Test
    void rejectsFinishingWhenPaymentIsPendingReview() {
        Context context = context("FINPAYMENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        markAll(contract, CuotaEstado.PAGADA);
        pendingPayment(cuotas(contract).getFirst(), context.usuario());
        assertThatThrownBy(() -> contratoService.finish(contract.codcon(), context.authentication()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pendientes de revisión");
    }

    @Test
    void finishesOnlyAfterDateWithAllQuotasPaid() {
        Context context = context("FINISH", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.minusMonths(1));
        markAll(contract, CuotaEstado.PAGADA);
        assertThat(contratoService.finish(contract.codcon(), context.authentication()).estado())
                .isEqualTo("FINALIZADO");
    }

    @Test
    void rejectsRescissionWhenEffectivePeriodIsPendingOrPartial() {
        Context context = context("RESPENDING", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.plusMonths(3));
        cuotas(contract).stream().filter(q -> q.getPeriodo().isBefore(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotaRepository.flush();
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inclusive");

        CuotaEntity current = cuotas(contract).stream().filter(q -> q.getPeriodo().equals(MES_ACTUAL))
                .findFirst().orElseThrow();
        current.setEstado(CuotaEstado.PARCIAL);
        cuotaRepository.saveAndFlush(current);
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("inclusive");
    }

    @Test
    void rescindsWhenPaidThroughEffectivePeriodAndAnnulsLaterQuotas() {
        Context context = context("RESCIND", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(2), MES_ACTUAL.plusMonths(3));
        cuotas(contract).stream().filter(q -> !q.getPeriodo().isAfter(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotas(contract).stream().filter(q -> q.getPeriodo().isAfter(MES_ACTUAL))
                .findFirst().orElseThrow().setEstado(CuotaEstado.PAGADA);
        cuotaRepository.flush();

        assertThat(rescind(contract, context).estado()).isEqualTo("RESCINDIDO");
        assertThat(cuotas(contract)).allSatisfy(q -> {
            if (q.getPeriodo().isAfter(MES_ACTUAL)) {
                assertThat(q.getEstado()).isEqualTo(CuotaEstado.ANULADA);
            }
        });
    }

    @Test
    void rejectsRescissionWhileAnyPaymentAwaitsReview() {
        Context context = context("RESPAYMENT", (short) 1, (short) 1, (short) 1);
        ContratoResponse contract = create(context, MES_ACTUAL.minusMonths(1), MES_ACTUAL.plusMonths(2));
        cuotas(contract).stream().filter(q -> !q.getPeriodo().isAfter(MES_ACTUAL))
                .forEach(q -> q.setEstado(CuotaEstado.PAGADA));
        cuotaRepository.flush();
        pendingPayment(cuotas(contract).getLast(), context.usuario());
        assertThatThrownBy(() -> rescind(contract, context))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pendientes de revisión");
    }

    private ContratoResponse create(Context context, LocalDate start, LocalDate end) {
        return contratoService.create(context.unidad().getCoduni(),
                request(context.inquilino().getCodper(), start, end, new BigDecimal("1500.00")),
                context.authentication());
    }

    private ContratoResponse rescind(ContratoResponse contract, Context context) {
        return contratoService.rescind(contract.codcon(),
                new RescisionContratoRequest(MES_ACTUAL, "Terminación anticipada acordada."),
                context.authentication());
    }

    private ContratoRequest request(Integer tenant, LocalDate start, LocalDate end, BigDecimal amount) {
        return new ContratoRequest(tenant, start, end, amount, new BigDecimal("500.00"));
    }

    private PageRequest page() {
        return PageRequest.of(0, 20);
    }

    private List<CuotaEntity> cuotas(ContratoResponse contract) {
        return cuotaRepository.findAllByContratoCodconOrderByPeriodoAsc(contract.codcon());
    }

    private void markAll(ContratoResponse contract, CuotaEstado estado) {
        cuotas(contract).forEach(q -> q.setEstado(estado));
        cuotaRepository.flush();
    }

    private void pendingPayment(CuotaEntity cuota, Usuario actor) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setMonto(new BigDecimal("10.00"));
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setFechaPago(LocalDateTime.now());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(actor);
        pago.setIdempotencyKey(UUID.randomUUID());
        pagoRepository.saveAndFlush(pago);
    }

    private Context context(String prefix, short propertyState, short unitState, short tenantState) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona owner = persona(prefix + "O" + token, (short) 1);
        Usuario user = usuario("ct." + prefix.toLowerCase() + "." + token, owner);
        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Propiedad " + prefix + token);
        property.setTipo("CASA");
        property.setDireccion("Calle de prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(owner);
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado(propertyState);
        property = propiedadRepository.saveAndFlush(property);
        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + prefix + token);
        unit.setTipoUnidad("DEPARTAMENTO");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("1500.00"));
        unit.setEstadoOperativo(unitState);
        unit = unidadRepository.saveAndFlush(unit);
        Persona tenant = persona(prefix + "T" + token, tenantState);
        return new Context(user, authentication(user), unit, tenant);
    }

    private Persona persona(String ci, short estado) {
        String normalized = ci.substring(0, Math.min(20, ci.length()));
        Persona persona = new Persona();
        persona.setCi(normalized);
        persona.setNombre("Persona contractual");
        persona.setGenero('F');
        persona.setEstado(estado);
        persona.setCorreo(normalized.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private Usuario usuario(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login.substring(0, Math.min(30, login.length())));
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(persona);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Authentication authentication(Usuario usuario) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private record Context(Usuario usuario, Authentication authentication, UnidadEntity unidad, Persona inquilino) {
    }
}
