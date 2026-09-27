package com.orman.backend.contract.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.contract.service.InquilinoPortalService;
import com.orman.backend.payment.dto.request.QrCobroRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.service.PagoService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
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
class InquilinoPortalIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoComprobanteRepository comprobanteRepository;
    @Autowired private InquilinoPortalService portalService;
    @Autowired private PagoService pagoService;
    @Autowired private QrCobroService qrCobroService;

    @Test
    void listsEveryContractStateAndHidesContractsOfAnotherTenant() {
        Context tenantA = context("PORTALA");
        Context tenantB = context("PORTALB");
        ContratoEntity vigente = contract(tenantA, ContratoEstado.VIGENTE, LocalDate.of(2026, 9, 1));
        ContratoEntity programado = contract(tenantA, ContratoEstado.PROGRAMADO, LocalDate.of(2027, 1, 1));
        ContratoEntity rescindido = contract(tenantA, ContratoEstado.RESCINDIDO, LocalDate.of(2026, 6, 1));
        ContratoEntity finalizado = contract(tenantA, ContratoEstado.FINALIZADO, LocalDate.of(2025, 6, 1));
        ContratoEntity otherTenantContract = contract(tenantB, ContratoEstado.VIGENTE, LocalDate.of(2026, 9, 1));

        var page = portalService.listContracts(PageRequest.of(0, 20), tenantA.authentication());

        assertThat(page.content()).extracting(row -> row.estado())
                .containsExactly("VIGENTE", "PROGRAMADO", "RESCINDIDO", "FINALIZADO");
        assertThat(page.content()).extracting(row -> row.codcon())
                .containsExactly(vigente.getCodcon(), programado.getCodcon(), rescindido.getCodcon(),
                        finalizado.getCodcon())
                .doesNotContain(otherTenantContract.getCodcon());
        assertThat(portalService.getContract(vigente.getCodcon(), tenantA.authentication()).codcon())
                .isEqualTo(vigente.getCodcon());
        assertThatThrownBy(() -> portalService.getContract(otherTenantContract.getCodcon(), tenantA.authentication()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(page.content().getFirst().nombrePropiedad()).isNotBlank();
        assertThat(page.content().getFirst().nombreUnidad()).isNotBlank();
    }

    @Test
    void quotaReadsShowAllStatesAndAggregateRealPaymentsWithoutReducingBalanceForReview() throws Exception {
        Context context = context("PORTALPAY");
        ContratoEntity contract = contract(context, ContratoEstado.VIGENTE, LocalDate.of(2026, 9, 1));
        CuotaEntity partial = quota(contract, LocalDate.of(2026, 9, 1), CuotaEstado.PENDIENTE,
                new BigDecimal("2500.00"));
        quota(contract, LocalDate.of(2026, 10, 1), CuotaEstado.PAGADA, new BigDecimal("2500.00"));
        quota(contract, LocalDate.of(2026, 11, 1), CuotaEstado.PENDIENTE, new BigDecimal("2500.00"));
        quota(contract, LocalDate.of(2026, 12, 1), CuotaEstado.ANULADA, new BigDecimal("2500.00"));

        pagoService.create(partial.getCodcuo(), payment("1000.00", MetodoPago.EFECTIVO, null), null,
                context.ownerAuthentication());
        LocalDate today = LocalDate.now(ZoneId.of("America/La_Paz"));
        qrCobroService.create(new QrCobroRequest(today.minusDays(1), today.plusYears(1)), png("portal-qr.png"),
                context.ownerAuthentication());
        var pending = pagoService.create(partial.getCodcuo(), payment("500.00", MetodoPago.QR,
                        LocalDateTime.now(ZoneId.of("America/La_Paz")).minusMinutes(1)), png("portal-proof.png"),
                context.authentication());
        assertThat(pending.estado()).isEqualTo("PENDIENTE_REVISION");
        assertThat(comprobanteRepository.existsByPagoCodpag(pending.codpag())).isTrue();

        var quotas = portalService.listQuotas(contract.getCodcon(), context.authentication());
        var partialResponse = portalService.getQuota(partial.getCodcuo(), context.authentication());

        assertThat(quotas).hasSize(4);
        assertThat(quotas).extracting(row -> row.estado())
                .containsExactly("PARCIAL", "PAGADA", "PENDIENTE", "ANULADA");
        assertThat(quotas).extracting(row -> row.periodo()).isSorted();
        assertThat(partialResponse.codcon()).isEqualTo(contract.getCodcon());
        assertThat(partialResponse.monto()).isEqualByComparingTo("2500.00");
        assertThat(partialResponse.montoConfirmado()).isEqualByComparingTo("1000.00");
        assertThat(partialResponse.montoPendienteRevision()).isEqualByComparingTo("500.00");
        assertThat(partialResponse.saldo()).isEqualByComparingTo("1500.00");
        assertThat(partialResponse.estado()).isEqualTo("PARCIAL");
        assertThatThrownBy(() -> portalService.getQuota(partial.getCodcuo(), contextForOtherTenant().authentication()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Context context(String prefix) {
        String token = Long.toUnsignedString(System.nanoTime(), 36);
        Persona owner = person(prefix + "O" + token, 'A');
        Usuario ownerUser = user("tenant.owner." + token, owner);
        Persona tenant = person(prefix + "T" + token, 'I');
        Usuario tenantUser = user("tenant.user." + token, tenant);
        return new Context(owner, ownerUser, tenant, tenantUser,
                authentication(ownerUser, "ROLE_PROPIETARIO"), authentication(tenantUser, "ROLE_INQUILINO"));
    }

    private Context contextForOtherTenant() {
        return context("OTHERPORTAL");
    }

    private ContratoEntity contract(Context context, ContratoEstado estado, LocalDate fechaInicio) {
        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Edificio " + UUID.randomUUID());
        property.setTipo("CASA");
        property.setDireccion("DirecciÃ³n de prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(context.owner());
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado((short) 1);
        property = propiedadRepository.saveAndFlush(property);

        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + UUID.randomUUID());
        unit.setTipoUnidad("DEPARTAMENTO");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("2500.00"));
        unit.setEstadoOperativo((short) 1);
        unit = unidadRepository.saveAndFlush(unit);

        ContratoEntity contract = new ContratoEntity();
        contract.setUnidad(unit);
        contract.setInquilino(context.tenant());
        contract.setFechaInicio(fechaInicio);
        contract.setFechaFin(fechaInicio.plusYears(1));
        contract.setMontoMensual(new BigDecimal("2500.00"));
        contract.setMoneda("BOB");
        contract.setGarantia(new BigDecimal("2500.00"));
        contract.setEstado(estado);
        contract.setFechaRegistro(LocalDateTime.now());
        if (estado == ContratoEstado.RESCINDIDO) {
            contract.setFechaRescision(fechaInicio);
            contract.setMotivoRescision("Contrato de prueba rescindido.");
        }
        return contratoRepository.saveAndFlush(contract);
    }

    private CuotaEntity quota(ContratoEntity contract, LocalDate period, CuotaEstado state, BigDecimal amount) {
        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(period);
        quota.setFechaVencimiento(period);
        quota.setMonto(amount);
        quota.setEstado(state);
        return cuotaRepository.saveAndFlush(quota);
    }

    private PagoRequest payment(String amount, MetodoPago method, LocalDateTime paymentDate) {
        return new PagoRequest(new BigDecimal(amount), method, paymentDate, UUID.randomUUID());
    }

    private MockMultipartFile png(String fileName) throws Exception {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 128, 128);
            graphics.setColor(Color.BLACK);
            for (int y = 8; y < 120; y += 8) {
                for (int x = 8; x < 120; x += 8) {
                    if (((x + y) / 8) % 2 == 0) {
                        graphics.fillRect(x, y, 8, 8);
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return new MockMultipartFile("comprobante", fileName, "image/png", bytes.toByteArray());
    }

    private Persona person(String ci, char type) {
        String normalized = ci.substring(0, Math.min(20, ci.length()));
        Persona person = new Persona();
        person.setCi(normalized);
        person.setNombre("Persona " + normalized);
        person.setGenero('F');
        person.setEstado((short) 1);
        person.setCorreo(normalized.toLowerCase() + "@example.test");
        person.setTelefono("70000000");
        person.setTipoPersona(type);
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

    private record Context(Persona owner, Usuario ownerUser, Persona tenant, Usuario tenantUser,
                           Authentication ownerAuthentication, Authentication authentication) {
    }
}
