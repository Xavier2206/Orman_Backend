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
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.repository.NotificacionRepository;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.notification.service.NotificacionService;
import com.orman.backend.notification.service.impl.CuotaNotificacionScheduler;
import com.orman.backend.payment.dto.request.PagoComprobanteRequest;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.PagoComprobanteService;
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
import java.util.List;
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
    @Autowired private NotificacionService notificacionService;
    @Autowired private NotificacionGeneracionService notificacionGeneracionService;
    @Autowired private CuotaNotificacionScheduler cuotaNotificacionScheduler;
    @Autowired private PagoService pagoService;
    @Autowired private PagoComprobanteService pagoComprobanteService;
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
        CuotaEntity overdue = cuota(context, LocalDate.of(2026, 9, 1), CuotaEstado.PARCIAL);
        CuotaEntity tomorrow = cuota(context, LocalDate.of(2026, 10, 1), CuotaEstado.PENDIENTE);
        CuotaEntity today = cuota(context, LocalDate.of(2026, 11, 1), CuotaEstado.PENDIENTE);
        CuotaEntity paid = cuota(context, LocalDate.of(2026, 8, 1), CuotaEstado.PAGADA);
        CuotaEntity annulled = cuota(context, LocalDate.of(2026, 7, 1), CuotaEstado.ANULADA);

        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 9, 30));
        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 9, 30));

        assertNotification(context.usuario(), NotificacionTipo.CUOTA_VENCIDA, ReferenciaTipo.CUOTA,
                overdue.getCodcuo());
        assertNotification(context.usuario(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                tomorrow.getCodcuo());
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, paid.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, annulled.getCodcuo())).isEmpty();
        assertThat(notificacionRepository.searchOwn(context.usuario().getLogin(), null, null,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements()).isEqualTo(2);

        cuotaNotificacionScheduler.generateFor(LocalDate.of(2026, 11, 1));
        assertThat(notificacionRepository.findByDestinatarioLoginAndTipoAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER, ReferenciaTipo.CUOTA,
                today.getCodcuo()).orElseThrow().getMensaje()).contains("vence hoy");
    }

    @Test
    void createsManualReminderOnlyForOwnedPendingOrPartialQuotaAndAvoidsDuplicates() {
        Context context = context("MANUAL");
        CuotaEntity pending = cuota(context, LocalDate.of(2026, 12, 1), CuotaEstado.PENDIENTE);
        Authentication ownerAuthentication = authentication(context.usuario());

        var first = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);
        var repeated = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);

        assertThat(first.codnot()).isEqualTo(repeated.codnot());
        assertThat(first.referenciaTipo()).isEqualTo("CUOTA");
        assertThat(first.mensaje()).isEqualTo("Le recordamos que su cuota de alquiler se encuentra pendiente de pago.");
        assertThat(notificacionRepository.findAllByDestinatarioLoginAndReferenciaTipoAndReferenciaId(
                context.usuario().getLogin(), ReferenciaTipo.CUOTA, pending.getCodcuo())).hasSize(1);

        Context other = context("OTHER");
        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(),
                authentication(other.usuario()))).isInstanceOf(AccessDeniedException.class);
        CuotaEntity paid = cuota(context, LocalDate.of(2026, 11, 1), CuotaEstado.PAGADA);
        assertThatThrownBy(() -> notificacionGeneracionService.notifyPendingPayment(paid.getCodcuo(), ownerAuthentication))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listsOwnNotificationsAndMarksThemReadIdempotently() {
        Context context = context("READ");
        CuotaEntity pending = cuota(context, LocalDate.of(2026, 12, 1), CuotaEstado.PENDIENTE);
        Authentication ownerAuthentication = authentication(context.usuario());
        var created = notificacionGeneracionService.notifyPendingPayment(pending.getCodcuo(), ownerAuthentication);

        assertThat(notificacionService.list(null, false,
                org.springframework.data.domain.PageRequest.of(0, 20), ownerAuthentication).content())
                .extracting(response -> response.codnot()).contains(created.codnot());
        assertThat(notificacionService.summary(ownerAuthentication).noLeidas()).isEqualTo(1);
        var firstRead = notificacionService.markAsRead(created.codnot(), ownerAuthentication);
        var repeatedRead = notificacionService.markAsRead(created.codnot(), ownerAuthentication);
        assertThat(firstRead.leida()).isTrue();
        assertThat(repeatedRead.fechaLectura()).isEqualTo(firstRead.fechaLectura());
        assertThat(notificacionService.summary(ownerAuthentication).noLeidas()).isZero();

        Context other = context("READ-OTHER");
        assertThatThrownBy(() -> notificacionService.get(created.codnot(), authentication(other.usuario())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createsNotificationsForPaymentConfirmationRejectionAndReceivedProof() {
        Context context = context("PAYMENT");
        Authentication ownerAuthentication = authentication(context.usuario());
        CuotaEntity confirmedQuota = cuota(context, LocalDate.of(2026, 9, 1), CuotaEstado.PENDIENTE);
        var confirmed = pagoService.create(confirmedQuota.getCodcuo(), paymentRequest(), ownerAuthentication);
        notificacionGeneracionService.generatePaymentConfirmed(confirmed.codpag());
        assertNotification(context.usuario(), NotificacionTipo.PAGO_CONFIRMADO, ReferenciaTipo.PAGO, confirmed.codpag());

        CuotaEntity rejectedQuota = cuota(context, LocalDate.of(2026, 10, 1), CuotaEstado.PENDIENTE);
        PagoEntity rejected = pendingPayment(context, rejectedQuota);
        pagoService.reject(rejected.getCodpag(), new PagoMotivoRequest("Comprobante inválido"), ownerAuthentication);
        notificacionGeneracionService.generatePaymentRejected(rejected.getCodpag());
        assertNotification(context.usuario(), NotificacionTipo.PAGO_RECHAZADO, ReferenciaTipo.PAGO, rejected.getCodpag());

        CuotaEntity proofQuota = cuota(context, LocalDate.of(2026, 11, 1), CuotaEstado.PENDIENTE);
        PagoEntity proofPayment = pendingPayment(context, proofQuota);
        pagoComprobanteService.create(proofPayment.getCodpag(), new PagoComprobanteRequest("https://example.test/pago.pdf",
                "pago.pdf", "application/pdf", 0), ownerAuthentication);
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
        return new PagoRequest(new BigDecimal("100.00"), MetodoPago.EFECTIVO, null,
                "REF-" + UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID());
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
        contrato.setInquilino(persona((prefix + "T" + token).substring(0, Math.min(20, prefix.length() + token.length() + 1))));
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
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setMonto(new BigDecimal("100.00"));
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
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(context.contrato());
        cuota.setPeriodo(periodo);
        cuota.setFechaVencimiento(periodo);
        cuota.setMonto(new BigDecimal("100.00"));
        cuota.setEstado(estado);
        return cuotaRepository.saveAndFlush(cuota);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona de notificación");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private Authentication authentication(Usuario usuario) {
        return new TestingAuthenticationToken(new AuthenticatedUser(usuario.getLogin(), UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
    }

    private record Context(Usuario usuario, ContratoEntity contrato) {
    }
}
