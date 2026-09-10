package com.orman.backend.payment.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.dto.request.CuentaPagoRequest;
import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.CuentaPagoResponse;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.service.CuentaPagoService;
import com.orman.backend.payment.service.PagoComprobanteService;
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
class PaymentModuleIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoService pagoService;
    @Autowired private PagoComprobanteService comprobanteService;
    @Autowired private ReciboService reciboService;
    @Autowired private CuentaPagoService cuentaPagoService;

    @Test
    void confirmsPartialAndTotalPaymentsGeneratingReceiptsAndUpdatingQuota() {
        Persona propietaria = createPersona("PM-OWNER-001");
        Authentication authentication = authentication(createUsuario("payment.owner", propietaria));
        CuotaEntity cuota = createCuota(propietaria, "PM-TENANT-001", new BigDecimal("1500.00"));
        CuentaPagoResponse cuenta = cuentaPagoService.create(new CuentaPagoRequest("Banco de prueba", "1001",
                "Titular", "https://example.test/qr.png", "Pagar con QR", 0, "1"), authentication);

        PagoResponse efectivo = pagoService.create(cuota.getCodcuo(), pago(new BigDecimal("1000.00"),
                MetodoPago.EFECTIVO, null), authentication);
        PagoResponse confirmadoParcial = pagoService.confirm(efectivo.codpag(), authentication);
        assertThat(confirmadoParcial.estado()).isEqualTo("CONFIRMADO");
        assertThat(cuotaRepository.findByCodcuo(cuota.getCodcuo()).orElseThrow().getEstado()).isEqualTo(CuotaEstado.PARCIAL);
        assertThat(reciboService.getByPago(efectivo.codpag(), authentication).codpag()).isEqualTo(efectivo.codpag());

        PagoResponse transferencia = pagoService.create(cuota.getCodcuo(), pago(new BigDecimal("500.00"),
                MetodoPago.TRANSFERENCIA, cuenta.codcta()), authentication);
        comprobanteService.create(transferencia.codpag(), new PagoComprobanteRequest("https://example.test/transfer.pdf",
                "transfer.pdf", "application/pdf", 0), authentication);
        pagoService.confirm(transferencia.codpag(), authentication);
        assertThat(cuotaRepository.findByCodcuo(cuota.getCodcuo()).orElseThrow().getEstado()).isEqualTo(CuotaEstado.PAGADA);
        assertThat(reciboService.getByPago(transferencia.codpag(), authentication).codpag())
                .isEqualTo(transferencia.codpag());
        assertThat(pagoService.listByCuota(cuota.getCodcuo(), authentication)).hasSize(2);
    }

    @Test
    void rejectsAndAnnulsOnlyPendingPaymentsAndProtectsOwnerResources() {
        Persona propietaria = createPersona("PM-OWNER-002");
        Authentication authentication = authentication(createUsuario("payment.owner.two", propietaria));
        CuotaEntity cuota = createCuota(propietaria, "PM-TENANT-002", new BigDecimal("1000.00"));
        PagoResponse rechazado = pagoService.create(cuota.getCodcuo(), pago(new BigDecimal("100.00"),
                MetodoPago.EFECTIVO, null), authentication);
        assertThat(pagoService.reject(rechazado.codpag(), new PagoMotivoRequest("Comprobante inválido"), authentication)
                .estado()).isEqualTo("RECHAZADO");
        PagoResponse anulable = pagoService.create(cuota.getCodcuo(), pago(new BigDecimal("100.00"),
                MetodoPago.EFECTIVO, null), authentication);
        assertThat(pagoService.annul(anulable.codpag(), new PagoMotivoRequest("Registro duplicado"), authentication)
                .estado()).isEqualTo("ANULADO");

        Persona otra = createPersona("PM-OTHER-001");
        Authentication otherAuthentication = authentication(createUsuario("payment.other", otra));
        assertThatThrownBy(() -> pagoService.get(rechazado.codpag(), otherAuthentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> cuentaPagoService.get(
                cuentaPagoService.create(new CuentaPagoRequest("Banco", "2002", "Titular", null, null, 0, "1"),
                        authentication).codcta(), otherAuthentication)).isInstanceOf(AccessDeniedException.class);
    }

    private PagoRequest pago(BigDecimal monto, MetodoPago metodo, Integer codcta) {
        return new PagoRequest(monto, metodo, codcta, "REF-" + UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID());
    }

    private CuotaEntity createCuota(Persona propietaria, String tenantCi, BigDecimal monto) {
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Propiedad de pagos " + tenantCi);
        propiedad.setTipo("CASA");
        propiedad.setDireccion("Calle de prueba");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(BigDecimal.ZERO);
        propiedad.setEstado((short) 1);
        propiedad = propiedadRepository.saveAndFlush(propiedad);
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Unidad de pagos " + tenantCi);
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("40.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(monto);
        unidad.setEstadoOperativo((short) 1);
        unidad = unidadRepository.saveAndFlush(unidad);
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(createPersona(tenantCi));
        contrato.setFechaInicio(LocalDate.of(2026, 9, 1));
        contrato.setFechaFin(LocalDate.of(2027, 9, 1));
        contrato.setMontoMensual(monto);
        contrato.setGarantia(BigDecimal.ZERO);
        contrato.setEstado(ContratoEstado.VIGENTE);
        contrato.setFechaConfirmacion(LocalDateTime.now());
        contrato = contratoRepository.saveAndFlush(contrato);
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(contrato);
        cuota.setPeriodo(LocalDate.of(2026, 9, 1));
        cuota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        cuota.setMonto(monto);
        cuota.setEstado(CuotaEstado.PENDIENTE);
        return cuotaRepository.saveAndFlush(cuota);
    }

    private Persona createPersona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona de pagos");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private Usuario createUsuario(String login, Persona persona) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
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
}
