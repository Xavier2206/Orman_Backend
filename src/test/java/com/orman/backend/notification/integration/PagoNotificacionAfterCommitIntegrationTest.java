package com.orman.backend.notification.integration;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.notification.entity.NotificacionTipo;
import com.orman.backend.notification.entity.ReferenciaTipo;
import com.orman.backend.notification.dto.response.NotificacionResponse;
import com.orman.backend.notification.service.NotificacionGeneracionService;
import com.orman.backend.notification.service.NotificacionService;
import com.orman.backend.notification.websocket.NotificacionDisponiblePayload;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.request.PagoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.QrCobroEntity;
import com.orman.backend.payment.entity.QrCobroEstado;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.QrCobroRepository;
import com.orman.backend.payment.service.PagoService;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
class PagoNotificacionAfterCommitIntegrationTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    @Autowired private Clock clock;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PersonaRepository personaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private QrCobroRepository qrCobroRepository;
    @Autowired private NotificacionGeneracionService notificacionGeneracionService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private PagoService pagoService;
    @Autowired private PagoComprobanteRepository pagoComprobanteRepository;
    @Autowired private PaymentImageStorageService imageStorageService;
    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    @Test
    void persistsReceivedProofNotificationForOwnerAfterPaymentCommit() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(registerTenantQrPayment(fixture)));

            assertThat(payment.get().estado()).isEqualTo("PENDIENTE_REVISION");
            assertThat(payment.get().origenRegistro()).isEqualTo("INQUILINO");
            assertThat(pagoComprobanteRepository.existsByPagoCodpag(payment.get().codpag())).isTrue();
            assertPersistedNotification(fixture.ownerLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    payment.get().codpag());
            assertWebSocketEvent(fixture.ownerLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    ReferenciaTipo.PAGO, payment.get().codpag());
            verifyNoMoreInteractions(messagingTemplate);
            assertNotificationAbsent(fixture.tenantLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    payment.get().codpag());

            commit(() -> notificacionGeneracionService.generateComprobanteReceived(payment.get().codpag()));
            assertNotificationCount(NotificacionTipo.COMPROBANTE_RECIBIDO, payment.get().codpag(), 1);
            verify(messagingTemplate, times(1)).convertAndSendToUser(ArgumentMatchers.eq(fixture.ownerLogin()),
                    ArgumentMatchers.eq("/queue/notificaciones"), ArgumentMatchers.any());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void persistsTenantPaymentConfirmationNotificationAfterCommit() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(registerTenantQrPayment(fixture)));
            reset(messagingTemplate);
            commit(() -> pagoService.confirm(payment.get().codpag(), fixture.ownerAuthentication()));

            assertPersistedNotification(fixture.tenantLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
            assertWebSocketEvent(fixture.tenantLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    ReferenciaTipo.PAGO, payment.get().codpag());
            verifyNoMoreInteractions(messagingTemplate);
            assertNotificationAbsent(fixture.ownerLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void persistsTenantPaymentRejectionNotificationAfterCommit() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(registerTenantQrPayment(fixture)));
            reset(messagingTemplate);
            commit(() -> pagoService.reject(payment.get().codpag(), new PagoMotivoRequest("Comprobante de prueba"),
                    fixture.ownerAuthentication()));

            assertPersistedNotification(fixture.tenantLogin(), NotificacionTipo.PAGO_RECHAZADO,
                    payment.get().codpag());
            assertWebSocketEvent(fixture.tenantLogin(), NotificacionTipo.PAGO_RECHAZADO,
                    ReferenciaTipo.PAGO, payment.get().codpag());
            verifyNoMoreInteractions(messagingTemplate);
            assertNotificationAbsent(fixture.ownerLogin(), NotificacionTipo.PAGO_RECHAZADO,
                    payment.get().codpag());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void skipsTenantDecisionNotificationWhenTenantUserIsInactiveAfterPresentation() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(registerTenantQrPayment(fixture)));
            reset(messagingTemplate);
            commit(() -> {
                Usuario tenant = usuarioRepository.findById(fixture.tenantLogin()).orElseThrow();
                tenant.setEstado((short) 0);
                usuarioRepository.saveAndFlush(tenant);
            });
            commit(() -> pagoService.confirm(payment.get().codpag(), fixture.ownerAuthentication()));

            assertNotificationAbsent(fixture.tenantLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
            assertNotificationAbsent(fixture.ownerLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
            verifyNoInteractions(messagingTemplate);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void skipsTenantDecisionNotificationWhenTenantPersonIsInactiveAfterPresentation() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(registerTenantQrPayment(fixture)));
            reset(messagingTemplate);
            commit(() -> {
                Persona tenant = personaRepository.findById(fixture.tenantPersonId()).orElseThrow();
                tenant.setEstado((short) 0);
                personaRepository.saveAndFlush(tenant);
            });
            commit(() -> pagoService.reject(payment.get().codpag(),
                    new PagoMotivoRequest("Comprobante de prueba"), fixture.ownerAuthentication()));

            assertNotificationAbsent(fixture.tenantLogin(), NotificacionTipo.PAGO_RECHAZADO,
                    payment.get().codpag());
            assertNotificationAbsent(fixture.ownerLogin(), NotificacionTipo.PAGO_RECHAZADO,
                    payment.get().codpag());
            verifyNoInteractions(messagingTemplate);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void preservesOwnerRecipientForOwnerEnteredConfirmedPaymentAfterCommit() {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            commit(() -> payment.set(pagoService.create(fixture.codcuo(),
                    new PagoRequest(new BigDecimal("100.00"), MetodoPago.EFECTIVO, null, UUID.randomUUID()), null,
                    fixture.ownerAuthentication())));

            assertThat(payment.get().origenRegistro()).isEqualTo("PROPIETARIA");
            assertThat(payment.get().estado()).isEqualTo("CONFIRMADO");
            assertPersistedNotification(fixture.ownerLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
            assertWebSocketEvent(fixture.ownerLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    ReferenciaTipo.PAGO, payment.get().codpag());
            assertNotificationAbsent(fixture.tenantLogin(), NotificacionTipo.PAGO_CONFIRMADO,
                    payment.get().codpag());
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void eventIsDeliveredOnlyAfterNotificationCommitAndNotAfterRollback() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            doAnswer(invocation -> {
                NotificacionDisponiblePayload payload = (NotificacionDisponiblePayload) invocation.getArgument(2);
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM notificaciones WHERE codnot = ?", Integer.class, payload.codnot());
                assertThat(count).isEqualTo(1);
                return null;
            }).when(messagingTemplate).convertAndSendToUser(ArgumentMatchers.anyString(),
                    ArgumentMatchers.anyString(), ArgumentMatchers.any());

            transactionTemplate.execute(status -> {
                payment.set(registerTenantQrPayment(fixture));
                verifyNoInteractions(messagingTemplate);
                return null;
            });
            assertPersistedNotification(fixture.ownerLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    payment.get().codpag());
            assertWebSocketEvent(fixture.ownerLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    ReferenciaTipo.PAGO, payment.get().codpag());

            reset(messagingTemplate);
            assertThatThrownBy(() -> transactionTemplate.execute(status -> {
                notificacionGeneracionService.generateOverdueQuota(fixture.codcuo());
                throw new IllegalStateException("rollback de prueba");
            })).isInstanceOf(IllegalStateException.class).hasMessage("rollback de prueba");
            verifyNoInteractions(messagingTemplate);
            assertQuotaNotificationCount(fixture.tenantLogin(), fixture.codcuo(), 0);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void manualReminderCreationAndReopeningSendOncePerUnreadTransition() {
        Fixture fixture = createFixture();
        AtomicReference<NotificacionResponse> response = new AtomicReference<>();
        try {
            commit(() -> response.set(notificacionGeneracionService.notifyPendingPayment(
                    fixture.codcuo(), fixture.ownerAuthentication())));
            assertQuotaWebSocketEvent(fixture.tenantLogin(), response.get().codnot(), response.get().tipo());

            commit(() -> notificacionService.markAsRead(response.get().codnot(), fixture.tenantAuthentication()));
            reset(messagingTemplate);
            commit(() -> response.set(notificacionGeneracionService.notifyPendingPayment(
                    fixture.codcuo(), fixture.ownerAuthentication())));
            assertQuotaWebSocketEvent(fixture.tenantLogin(), response.get().codnot(), response.get().tipo());

            reset(messagingTemplate);
            commit(() -> notificacionGeneracionService.notifyPendingPayment(
                    fixture.codcuo(), fixture.ownerAuthentication()));
            verifyNoInteractions(messagingTemplate);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void upcomingAndOverdueQuotaCreationNotifyTheTenantPrivately() {
        Fixture fixture = createFixture();
        try {
            commit(() -> notificacionGeneracionService.generateUpcomingQuota(fixture.codcuo(),
                    LocalDate.now(LA_PAZ)));
            assertWebSocketEvent(fixture.tenantLogin(), NotificacionTipo.CUOTA_PROXIMA_VENCER,
                    ReferenciaTipo.CUOTA, fixture.codcuo());
            verifyNoMoreInteractions(messagingTemplate);

            reset(messagingTemplate);
            commit(() -> notificacionGeneracionService.generateOverdueQuota(fixture.codcuo()));
            assertWebSocketEvent(fixture.tenantLogin(), NotificacionTipo.CUOTA_VENCIDA,
                    ReferenciaTipo.CUOTA, fixture.codcuo());
            verifyNoMoreInteractions(messagingTemplate);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void websocketTransportFailureDoesNotUndoPersistedNotification() throws Exception {
        Fixture fixture = createFixture();
        AtomicReference<PagoResponse> payment = new AtomicReference<>();
        try {
            doThrow(new IllegalStateException("transport unavailable")).when(messagingTemplate)
                    .convertAndSendToUser(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                            ArgumentMatchers.any());

            commit(() -> payment.set(registerTenantQrPayment(fixture)));

            assertPersistedNotification(fixture.ownerLogin(), NotificacionTipo.COMPROBANTE_RECIBIDO,
                    payment.get().codpag());
            verify(messagingTemplate).convertAndSendToUser(ArgumentMatchers.eq(fixture.ownerLogin()),
                    ArgumentMatchers.eq("/queue/notificaciones"), ArgumentMatchers.any());
        } finally {
            cleanup(fixture);
        }
    }

    private Fixture createFixture() {
        Fixture fixture = transactionTemplate.execute(status -> {
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Persona owner = createPerson("AO" + token, "Propietario after commit", 'A');
            Usuario ownerUser = createUser("after.owner." + token, owner);
            Persona tenant = createPerson("AT" + token, "Inquilino after commit", 'I');
            Usuario tenantUser = createUser("after.tenant." + token, tenant);

            PropiedadEntity property = new PropiedadEntity();
            property.setNombre("Propiedad after commit " + token);
            property.setTipo("CASA");
            property.setDireccion("Calle de prueba");
            property.setCiudad("La Paz");
            property.setPropietaria(owner);
            property.setInversionInicial(BigDecimal.ZERO);
            property.setEstado((short) 1);
            property = propiedadRepository.saveAndFlush(property);

            UnidadEntity unit = new UnidadEntity();
            unit.setPropiedad(property);
            unit.setNombre("Unidad after commit " + token);
            unit.setTipoUnidad("DEPARTAMENTO");
            unit.setArea(new BigDecimal("40.00"));
            unit.setDormitorios((short) 1);
            unit.setBanos((short) 1);
            unit.setPiso(1);
            unit.setPrecioBase(new BigDecimal("1000.00"));
            unit.setEstadoOperativo((short) 1);
            unit = unidadRepository.saveAndFlush(unit);

            LocalDate period = LocalDate.now(LA_PAZ).withDayOfMonth(1).minusMonths(1);
            ContratoEntity contract = new ContratoEntity();
            contract.setUnidad(unit);
            contract.setInquilino(tenant);
            contract.setFechaInicio(LocalDate.of(2026, 1, 1));
            contract.setFechaFin(LocalDate.of(2028, 1, 1));
            contract.setMontoMensual(new BigDecimal("1000.00"));
            contract.setMoneda("BOB");
            contract.setGarantia(BigDecimal.ZERO);
            contract.setEstado(ContratoEstado.VIGENTE);
            contract.setFechaRegistro(nowUtc());
            contract = contratoRepository.saveAndFlush(contract);

            CuotaEntity quota = new CuotaEntity();
            quota.setContrato(contract);
            quota.setPeriodo(period);
            quota.setFechaVencimiento(period);
            quota.setMonto(new BigDecimal("1000.00"));
            quota.setEstado(CuotaEstado.PENDIENTE);
            quota = cuotaRepository.saveAndFlush(quota);

            QrCobroEntity qr = new QrCobroEntity();
            qr.setPropietaria(owner);
            qr.setRutaArchivo("qr-cobro/" + owner.getCodper() + "/00000000-0000-0000-0000-000000000001.png");
            qr.setNombreArchivo("qr-test.png");
            qr.setTipoContenido("image/png");
            qr.setFechaInicio(LocalDate.of(2020, 1, 1));
            qr.setFechaFin(LocalDate.of(2035, 12, 31));
            qr.setEstado(QrCobroEstado.ACTIVO);
            qr.setFechaRegistro(nowUtc());
            qr = qrCobroRepository.saveAndFlush(qr);

            return new Fixture(owner.getCodper(), tenant.getCodper(), ownerUser.getLogin(), tenantUser.getLogin(),
                    property.getCodprop(), unit.getCoduni(), contract.getCodcon(), quota.getCodcuo(), qr.getCodqr(),
                    authentication(ownerUser.getLogin(), "ROLE_PROPIETARIO"),
                    authentication(tenantUser.getLogin(), "ROLE_INQUILINO"));
        });
        if (fixture == null) {
            throw new IllegalStateException("No se pudo crear el fixture transaccional.");
        }
        return fixture;
    }

    private Persona createPerson(String ci, String name, Character type) {
        Persona person = new Persona();
        person.setCi(ci);
        person.setNombre(name);
        person.setGenero('F');
        person.setEstado((short) 1);
        person.setCorreo(ci.toLowerCase() + "@example.test");
        person.setTelefono("70000000");
        person.setTipoPersona(type);
        person.setFechaRegistro(nowUtc());
        return personaRepository.saveAndFlush(person);
    }

    private Usuario createUser(String login, Persona person) {
        Usuario user = new Usuario();
        user.setLogin(login);
        user.setPasswd("test-password-hash");
        user.setEstado((short) 1);
        user.setPersona(person);
        user.setFechaCreacion(nowUtc());
        return usuarioRepository.saveAndFlush(user);
    }

    private PagoResponse registerTenantQrPayment(Fixture fixture) {
        LocalDateTime fechaPago = LocalDateTime.ofInstant(clock.instant(), LA_PAZ);
        return pagoService.create(fixture.codcuo(),
                new PagoRequest(new BigDecimal("100.00"), MetodoPago.QR, fechaPago, UUID.randomUUID()),
                pngProof(), fixture.tenantAuthentication());
    }

    private MockMultipartFile pngProof() {
        try {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new MockMultipartFile("comprobante", "after-commit-proof.png", "image/png", output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void assertPersistedNotification(String login, NotificacionTipo tipo, Integer codpag) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT login_destinatario, tipo, referencia_tipo, referencia_id
                FROM notificaciones
                WHERE login_destinatario = ? AND tipo = ? AND referencia_tipo = ? AND referencia_id = ?
                """, login, tipo.name(), ReferenciaTipo.PAGO.name(), codpag);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("login_destinatario")).isEqualTo(login);
        assertThat(row.get("tipo")).isEqualTo(tipo.name());
        assertThat(row.get("referencia_tipo")).isEqualTo(ReferenciaTipo.PAGO.name());
        assertThat(row.get("referencia_id")).isEqualTo(codpag);
    }

    private void assertNotificationAbsent(String login, NotificacionTipo tipo, Integer codpag) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificaciones
                WHERE login_destinatario = ? AND tipo = ? AND referencia_tipo = ? AND referencia_id = ?
                """, Integer.class, login, tipo.name(), ReferenciaTipo.PAGO.name(), codpag);
        assertThat(count).isZero();
    }

    private void assertNotificationCount(NotificacionTipo tipo, Integer codpag, int expected) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificaciones
                WHERE tipo = ? AND referencia_tipo = ? AND referencia_id = ?
                """, Integer.class, tipo.name(), ReferenciaTipo.PAGO.name(), codpag);
        assertThat(count).isEqualTo(expected);
    }

    private void assertQuotaNotificationCount(String login, Integer codcuo, int expected) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM notificaciones
                WHERE login_destinatario = ? AND referencia_tipo = 'CUOTA' AND referencia_id = ?
                """, Integer.class, login, codcuo);
        assertThat(count).isEqualTo(expected);
    }

    private void assertWebSocketEvent(String login, NotificacionTipo tipo, ReferenciaTipo referenceType,
                                      Integer referenceId) {
        Long codnot = jdbcTemplate.queryForObject("""
                SELECT codnot FROM notificaciones
                WHERE login_destinatario = ? AND tipo = ? AND referencia_tipo = ? AND referencia_id = ?
                """, Long.class, login, tipo.name(), referenceType.name(), referenceId);
        ArgumentCaptor<NotificacionDisponiblePayload> payload = ArgumentCaptor.forClass(
                NotificacionDisponiblePayload.class);
        verify(messagingTemplate).convertAndSendToUser(ArgumentMatchers.eq(login),
                ArgumentMatchers.eq("/queue/notificaciones"), payload.capture());
        assertThat(payload.getValue()).isEqualTo(new NotificacionDisponiblePayload(codnot, tipo));
        verifyNoMoreInteractions(messagingTemplate);
    }

    private void assertQuotaWebSocketEvent(String login, Long codnot, String typeName) {
        ArgumentCaptor<NotificacionDisponiblePayload> payload = ArgumentCaptor.forClass(
                NotificacionDisponiblePayload.class);
        verify(messagingTemplate).convertAndSendToUser(ArgumentMatchers.eq(login),
                ArgumentMatchers.eq("/queue/notificaciones"), payload.capture());
        assertThat(payload.getValue()).isEqualTo(new NotificacionDisponiblePayload(codnot,
                NotificacionTipo.valueOf(typeName)));
        verifyNoMoreInteractions(messagingTemplate);
    }

    private void commit(Runnable operation) {
        transactionTemplate.execute(status -> {
            operation.run();
            return null;
        });
    }

    private void cleanup(Fixture fixture) {
        transactionTemplate.execute(status -> {
            List<Integer> codpags = jdbcTemplate.queryForList(
                    "SELECT codpag FROM pagos WHERE codcuo = ?", Integer.class, fixture.codcuo());
            for (Integer codpag : codpags) {
                pagoComprobanteRepository.findByPagoCodpag(codpag).ifPresent(comprobante ->
                        imageStorageService.deleteProofAfterCommit(comprobante.getRutaArchivo(), codpag));
                jdbcTemplate.update("DELETE FROM notificaciones WHERE referencia_tipo = 'PAGO' AND referencia_id = ?",
                        codpag);
                jdbcTemplate.update("DELETE FROM pago_comprobantes WHERE codpag = ?", codpag);
                jdbcTemplate.update("DELETE FROM pagos WHERE codpag = ?", codpag);
            }
            jdbcTemplate.update("DELETE FROM notificaciones WHERE referencia_tipo = 'CUOTA' AND referencia_id = ?",
                    fixture.codcuo());
            jdbcTemplate.update("DELETE FROM cuotas WHERE codcuo = ?", fixture.codcuo());
            jdbcTemplate.update("DELETE FROM contratos WHERE codcon = ?", fixture.codcon());
            jdbcTemplate.update("DELETE FROM qr_cobro WHERE codqr = ?", fixture.codqr());
            jdbcTemplate.update("DELETE FROM unidades WHERE coduni = ?", fixture.coduni());
            jdbcTemplate.update("DELETE FROM propiedades WHERE codprop = ?", fixture.codprop());
            jdbcTemplate.update("DELETE FROM usuarios WHERE login IN (?, ?)",
                    fixture.ownerLogin(), fixture.tenantLogin());
            jdbcTemplate.update("DELETE FROM personas WHERE codper IN (?, ?)",
                    fixture.ownerPersonId(), fixture.tenantPersonId());
            return null;
        });
    }

    private Authentication authentication(String login, String role) {
        return new TestingAuthenticationToken(new AuthenticatedUser(login, UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record Fixture(Integer ownerPersonId, Integer tenantPersonId, String ownerLogin, String tenantLogin,
                           Integer codprop, Integer coduni, Integer codcon, Integer codcuo, Integer codqr,
                           Authentication ownerAuthentication, Authentication tenantAuthentication) {
    }
}
