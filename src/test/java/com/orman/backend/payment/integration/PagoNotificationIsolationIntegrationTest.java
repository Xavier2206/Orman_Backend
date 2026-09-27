package com.orman.backend.payment.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.repository.PagoRepository;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class PagoNotificationIsolationIntegrationTest {

    @Autowired private PagoService pagoService;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private NotificacionGeneracionService notificacionGeneracionService;

    @Test
    void notificationFailureAfterCommitDoesNotRollbackPaymentOrQuota() {
        TestContext context = transactionTemplate.execute(status -> createContext());
        doThrow(new IllegalStateException("notification unavailable"))
                .when(notificacionGeneracionService).generatePaymentConfirmed(org.mockito.ArgumentMatchers.anyInt());

        PagoResponse[] created = new PagoResponse[1];
        try {
            assertThatCode(() -> created[0] = transactionTemplate.execute(status -> pagoService.create(
                    context.codcuo(), new PagoRequest(new BigDecimal("100.00"), MetodoPago.EFECTIVO,
                            null, UUID.randomUUID()), null, context.authentication())))
                    .doesNotThrowAnyException();

            assertThat(created[0]).isNotNull();
            assertThat(pagoRepository.findById(created[0].codpag()).orElseThrow().getEstado().name())
                    .isEqualTo("CONFIRMADO");
            assertThat(cuotaRepository.findById(context.codcuo()).orElseThrow().getEstado())
                    .isEqualTo(CuotaEstado.PAGADA);
        } finally {
            cleanup(context, created[0]);
        }
    }

    private TestContext createContext() {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona owner = persona("ISO-O-" + token);
        Persona tenant = persona("ISO-T-" + token);
        Usuario usuario = new Usuario();
        usuario.setLogin(("iso." + token).substring(0, Math.min(30, token.length() + 4)));
        usuario.setPasswd("hash-no-expuesto");
        usuario.setEstado((short) 1);
        usuario.setPersona(owner);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario = usuarioRepository.saveAndFlush(usuario);

        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Propiedad aislamiento " + token);
        property.setTipo("CASA");
        property.setDireccion("Calle prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(owner);
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado((short) 1);
        property = propiedadRepository.saveAndFlush(property);

        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad aislamiento");
        unit.setTipoUnidad("CASA");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("100.00"));
        unit.setEstadoOperativo((short) 1);
        unit = unidadRepository.saveAndFlush(unit);

        ContratoEntity contract = new ContratoEntity();
        contract.setUnidad(unit);
        contract.setInquilino(tenant);
        contract.setFechaInicio(LocalDate.now().withDayOfMonth(1));
        contract.setFechaFin(contract.getFechaInicio().plusMonths(1));
        contract.setMontoMensual(new BigDecimal("100.00"));
        contract.setMoneda("BOB");
        contract.setGarantia(BigDecimal.ZERO);
        contract.setEstado(ContratoEstado.VIGENTE);
        contract.setFechaRegistro(LocalDateTime.now());
        contract = contratoRepository.saveAndFlush(contract);

        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(contract.getFechaInicio());
        quota.setFechaVencimiento(contract.getFechaInicio());
        quota.setMonto(new BigDecimal("100.00"));
        quota.setEstado(CuotaEstado.PENDIENTE);
        quota = cuotaRepository.saveAndFlush(quota);

        Authentication auth = new TestingAuthenticationToken(
                new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
        return new TestContext(owner.getCodper(), tenant.getCodper(), usuario.getLogin(), property.getCodprop(),
                unit.getCoduni(), contract.getCodcon(), quota.getCodcuo(), auth);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci.substring(0, Math.min(20, ci.length())));
        persona.setNombre("Persona aislamiento");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private void cleanup(TestContext context, PagoResponse payment) {
        if (payment != null) {
            jdbcTemplate.update("DELETE FROM pagos WHERE codpag = ?", payment.codpag());
        }
        jdbcTemplate.update("DELETE FROM cuotas WHERE codcuo = ?", context.codcuo());
        jdbcTemplate.update("DELETE FROM contratos WHERE codcon = ?", context.codcon());
        jdbcTemplate.update("DELETE FROM unidades WHERE coduni = ?", context.coduni());
        jdbcTemplate.update("DELETE FROM propiedades WHERE codprop = ?", context.codprop());
        jdbcTemplate.update("DELETE FROM usuarios WHERE login = ?", context.login());
        jdbcTemplate.update("DELETE FROM personas WHERE codper IN (?, ?)", context.ownerId(), context.tenantId());
    }

    private record TestContext(Integer ownerId, Integer tenantId, String login, Integer codprop, Integer coduni,
                               Integer codcon, Integer codcuo, Authentication authentication) {
    }
}
