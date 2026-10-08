package com.orman.backend.dashboard.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.config.OrmanTimeConfig;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.dashboard.service.DashboardService;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback
class DashboardModuleIntegrationTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private Clock clock;

    @Test
    void totalsConfirmedPartialPaymentsPerPropertyAndDoesNotLeakAnotherOwner() {
        Owner firstOwner = createOwner("first");
        Owner secondOwner = createOwner("second");
        PropertyRoute firstProperty = createProperty(firstOwner, "Propiedad A", "200.00");
        PropertyRoute secondProperty = createProperty(firstOwner, "Propiedad B", "800.00");
        PropertyRoute otherProperty = createProperty(secondOwner, "Propiedad ajena", "500.00");

        createPayment(firstProperty, firstOwner.user(), "50.00", PagoEstado.CONFIRMADO, now());
        createPayment(firstProperty, firstOwner.user(), "25.00", PagoEstado.CONFIRMADO, now());
        createPayment(firstProperty, firstOwner.user(), "200.00", PagoEstado.PENDIENTE_REVISION, null);
        createPayment(firstProperty, firstOwner.user(), "75.00", PagoEstado.RECHAZADO, now());
        createPayment(firstProperty, firstOwner.user(), "30.00", PagoEstado.ANULADO, now());
        createPayment(secondProperty, firstOwner.user(), "400.00", PagoEstado.CONFIRMADO, now());
        createPayment(otherProperty, secondOwner.user(), "900.00", PagoEstado.CONFIRMADO, now());

        var firstReport = dashboardService.resumenFinanciero(authentication(firstOwner.user()));
        var secondReport = dashboardService.resumenFinanciero(authentication(secondOwner.user()));

        assertThat(firstReport.resumenGeneral().cantidadPropiedades()).isEqualTo(2);
        assertThat(firstReport.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("1000.00");
        assertThat(firstReport.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("475.00");
        assertThat(firstReport.resumenGeneral().porcentajeRecuperacion()).isEqualByComparingTo("47.50");
        assertThat(firstReport.propiedades()).extracting("ingresosConfirmadosAcumulados")
                .containsExactly(new BigDecimal("75.00"), new BigDecimal("400.00"));
        assertThat(firstReport.propiedades()).extracting("porcentajeRecuperacion")
                .containsExactly(new BigDecimal("37.50"), new BigDecimal("50.00"));
        assertThat(secondReport.resumenGeneral().cantidadPropiedades()).isEqualTo(1);
        assertThat(secondReport.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("900.00");
        assertThat(firstReport.propiedades()).extracting("nombre")
                .doesNotContain("Propiedad ajena");
    }

    @Test
    void handlesNoPropertiesZeroInvestmentAndRecoveryAboveOneHundredPercent() {
        Owner emptyOwner = createOwner("empty");
        Owner owner = createOwner("zero-and-recovered");
        PropertyRoute zeroInvestment = createProperty(owner, "Sin inversión", "0.00");
        PropertyRoute recovered = createProperty(owner, "Recuperación superior", "10.00");
        createPayment(recovered, owner.user(), "25.00", PagoEstado.CONFIRMADO, now());

        var emptyReport = dashboardService.resumenFinanciero(authentication(emptyOwner.user()));
        var report = dashboardService.resumenFinanciero(authentication(owner.user()));

        assertThat(emptyReport.resumenGeneral().cantidadPropiedades()).isZero();
        assertThat(emptyReport.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("0.00");
        assertThat(emptyReport.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("0.00");
        assertThat(emptyReport.resumenGeneral().porcentajeRecuperacion()).isNull();
        assertThat(emptyReport.propiedades()).isEmpty();
        assertThat(report.resumenGeneral().inversionInicialTotal()).isEqualByComparingTo("10.00");
        assertThat(report.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("25.00");
        assertThat(report.resumenGeneral().porcentajeRecuperacion()).isEqualByComparingTo("250.00");
        assertThat(report.propiedades()).filteredOn(property -> property.nombre().startsWith("Sin inversión"))
                .singleElement().satisfies(property -> {
                    assertThat(property.porcentajeRecuperacion()).isNull();
                    assertThat(property.ingresosConfirmadosAcumulados()).isEqualByComparingTo("0.00");
                });
        assertThat(report.propiedades()).filteredOn(property -> property.nombre().startsWith("Recuperación superior"))
                .singleElement().extracting(property -> property.porcentajeRecuperacion())
                .isEqualTo(new BigDecimal("250.00"));
    }

    @Test
    void returnsTwelveMonthlyBucketsUsingConfirmationDateWhileHistoricalTotalRemainsUnbounded() {
        Owner owner = createOwner("monthly");
        PropertyRoute property = createProperty(owner, "Serie mensual", "100.00");
        LocalDateTime now = now();
        createPayment(property, owner.user(), "20.00", PagoEstado.CONFIRMADO, now);
        createPayment(property, owner.user(), "70.00", PagoEstado.CONFIRMADO, now.minusMonths(15));

        var report = dashboardService.resumenFinanciero(authentication(owner.user()));
        YearMonth currentMonth = YearMonth.now(clock.withZone(OrmanTimeConfig.ORMAN_ZONE));

        assertThat(report.resumenGeneral().ingresosConfirmadosAcumulados()).isEqualByComparingTo("90.00");
        assertThat(report.ingresosConfirmadosPorMes()).hasSize(12);
        assertThat(report.ingresosConfirmadosPorMes().getFirst().periodo())
                .isEqualTo(currentMonth.minusMonths(11).toString());
        assertThat(report.ingresosConfirmadosPorMes().getLast().periodo()).isEqualTo(currentMonth.toString());
        assertThat(report.ingresosConfirmadosPorMes().stream().map(month -> month.monto())
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("20.00");
        assertThat(report.propiedades().getFirst().ingresosConfirmadosPorMes()).hasSize(12);
    }

    private Owner createOwner(String suffix) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Persona owner = persona("O" + token, "Propietario " + suffix, "owner." + token);
        Usuario user = new Usuario();
        user.setLogin("dashboard." + token);
        user.setPasswd(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
        user.setEstado((short) 1);
        user.setPersona(owner);
        user.setFechaCreacion(now());
        return new Owner(owner, usuarioRepository.saveAndFlush(user));
    }

    private PropertyRoute createProperty(Owner owner, String name, String investment) {
        PropiedadEntity property = new PropiedadEntity();
        property.setNombre(name + " " + UUID.randomUUID().toString().substring(0, 8));
        property.setTipo("CASA");
        property.setDireccion("Dirección de integración");
        property.setCiudad("La Paz");
        property.setPropietaria(owner.persona());
        property.setInversionInicial(new BigDecimal(investment));
        property.setEstado((short) 1);
        property = propiedadRepository.saveAndFlush(property);

        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + UUID.randomUUID().toString().substring(0, 8));
        unit.setTipoUnidad("CASA");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(0);
        unit.setPrecioBase(new BigDecimal("2500.00"));
        unit.setEstadoOperativo((short) 1);
        unit = unidadRepository.saveAndFlush(unit);

        LocalDate monthStart = YearMonth.now(clock.withZone(OrmanTimeConfig.ORMAN_ZONE)).atDay(1);
        ContratoEntity contract = new ContratoEntity();
        contract.setUnidad(unit);
        contract.setInquilino(persona("T" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "Inquilino", "tenant." + UUID.randomUUID().toString().substring(0, 8)));
        contract.setFechaInicio(monthStart);
        contract.setFechaFin(monthStart.plusYears(2));
        contract.setMontoMensual(new BigDecimal("2500.00"));
        contract.setMoneda("BOB");
        contract.setGarantia(BigDecimal.ZERO.setScale(2));
        contract.setEstado(ContratoEstado.VIGENTE);
        contract.setFechaRegistro(now());
        contract = contratoRepository.saveAndFlush(contract);

        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(monthStart);
        quota.setFechaVencimiento(monthStart);
        quota.setMonto(new BigDecimal("100000.00"));
        quota.setEstado(CuotaEstado.PENDIENTE);
        quota = cuotaRepository.saveAndFlush(quota);
        return new PropertyRoute(property, quota);
    }

    private Persona persona(String ci, String nombre, String mailPrefix) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre(nombre);
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(mailPrefix + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private PagoEntity createPayment(PropertyRoute route, Usuario owner, String amount,
                                     PagoEstado state, LocalDateTime confirmationDate) {
        PagoEntity payment = new PagoEntity();
        payment.setCuota(route.quota());
        payment.setMonto(new BigDecimal(amount));
        payment.setMetodo(MetodoPago.EFECTIVO);
        payment.setFechaPago(confirmationDate == null ? now() : confirmationDate);
        payment.setFechaRegistro(now());
        payment.setEstado(state);
        payment.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        payment.setRegistradoPor(owner);
        payment.setIdempotencyKey(UUID.randomUUID());
        if (state != PagoEstado.PENDIENTE_REVISION) {
            payment.setFechaRevision(confirmationDate);
            payment.setRevisadoPor(owner);
        }
        if (state == PagoEstado.RECHAZADO) {
            payment.setMotivoRechazo("Rechazo de prueba");
        }
        if (state == PagoEstado.ANULADO) {
            payment.setMotivoAnulacion("Anulación de prueba");
        }
        return pagoRepository.saveAndFlush(payment);
    }

    private Authentication authentication(Usuario user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private LocalDateTime now() {
        return OrmanTimeConfig.businessNow(clock);
    }

    private record Owner(Persona persona, Usuario user) {
    }

    private record PropertyRoute(PropiedadEntity property, CuotaEntity quota) {
    }

}
