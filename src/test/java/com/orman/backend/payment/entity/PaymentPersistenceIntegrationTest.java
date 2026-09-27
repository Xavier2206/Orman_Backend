package com.orman.backend.payment.entity;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.repository.QrCobroRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Rollback
class PaymentPersistenceIntegrationTest {

    @Autowired private PersonaRepository personaRepository;
    @Autowired private PropiedadRepository propiedadRepository;
    @Autowired private UnidadRepository unidadRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private CuotaRepository cuotaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PagoComprobanteRepository comprobanteRepository;
    @Autowired private QrCobroRepository qrCobroRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayV20CreatesQrModelAndRemovesAccountsAndReceipts() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);

        assertThat(tables).contains("qr_cobro", "pagos", "pago_comprobantes")
                .doesNotContain("cuentas_pago", "recibos");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '20' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'pagos'::regclass", String.class))
                .contains("pk_pagos", "fk_pagos_cuotas", "fk_pagos_qr_cobro", "uk_pagos_idempotency_key",
                        "ck_pagos_monto", "ck_pagos_metodo", "ck_pagos_qr_por_metodo",
                        "ck_pagos_origen_registro", "fk_pagos_usuarios_registrador", "fk_pagos_usuarios_revisor");
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'pago_comprobantes'::regclass", String.class))
                .contains("uk_pago_comprobantes_codpag", "ck_pago_comprobantes_tipo_contenido");
    }

    @Test
    void persistsQrPaymentAndSinglePrivateProofReference() {
        Fixture fixture = fixture("PERSIST-1");
        QrCobroEntity qr = qrCobroRepository.saveAndFlush(qr(fixture.owner()));
        PagoEntity pago = payment(fixture, qr, MetodoPago.QR);
        pago = pagoRepository.saveAndFlush(pago);
        PagoComprobanteEntity proof = new PagoComprobanteEntity();
        proof.setPago(pago);
        proof.setRutaArchivo("comprobantes/" + pago.getCodpag() + "/00000000-0000-0000-0000-000000000001.png");
        proof.setNombreArchivo("captura.png");
        proof.setTipoContenido("image/png");
        proof.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 18, 0));
        comprobanteRepository.saveAndFlush(proof);

        assertThat(pagoRepository.findById(pago.getCodpag()).orElseThrow().getQrCobro().getCodqr())
                .isEqualTo(qr.getCodqr());
        assertThat(comprobanteRepository.findByPagoCodpag(pago.getCodpag()).orElseThrow().getRutaArchivo())
                .contains("comprobantes/").doesNotContain("C:\\", "/home/");
    }

    @Test
    void databaseRejectsASecondProofForTheSamePayment() {
        Fixture fixture = fixture("PROOF-UNIQUE");
        QrCobroEntity qr = qrCobroRepository.saveAndFlush(qr(fixture.owner()));
        PagoEntity pago = pagoRepository.saveAndFlush(payment(fixture, qr, MetodoPago.QR));
        comprobanteRepository.saveAndFlush(proof(pago, "00000000-0000-0000-0000-000000000001.png"));
        assertThatThrownBy(() -> comprobanteRepository.saveAndFlush(
                proof(pago, "00000000-0000-0000-0000-000000000002.png")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsTransferMethod() {
        Fixture fixture = fixture("PERSIST-2");
        QrCobroEntity qr = qrCobroRepository.saveAndFlush(qr(fixture.owner()));
        String sql = """
                INSERT INTO pagos (codcuo, codqr, monto, metodo, fecha_pago, fecha_registro, estado,
                    origen_registro, idempotency_key, registrado_por)
                VALUES (?, ?, 10, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    'PENDIENTE_REVISION', 'PROPIETARIA', ?, ?)
                """;
        assertThatThrownBy(() -> jdbcTemplate.update(sql, fixture.quota().getCodcuo(), qr.getCodqr(),
                "TRANSFERENCIA", UUID.randomUUID(), fixture.user().getLogin()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsCashAssociatedWithQr() {
        Fixture fixture = fixture("PERSIST-4");
        QrCobroEntity qr = qrCobroRepository.saveAndFlush(qr(fixture.owner()));
        String sql = """
                INSERT INTO pagos (codcuo, codqr, monto, metodo, fecha_pago, fecha_registro, estado,
                    origen_registro, idempotency_key, registrado_por)
                VALUES (?, ?, 10, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    'PENDIENTE_REVISION', 'PROPIETARIA', ?, ?)
                """;
        assertThatThrownBy(() -> jdbcTemplate.update(sql, fixture.quota().getCodcuo(), qr.getCodqr(),
                "EFECTIVO", UUID.randomUUID(), fixture.user().getLogin()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseAllowsCashWithoutQr() {
        Fixture fixture = fixture("PERSIST-3");
        PagoEntity cash = payment(fixture, null, MetodoPago.EFECTIVO);
        assertThat(pagoRepository.saveAndFlush(cash).getQrCobro()).isNull();
    }

    private QrCobroEntity qr(Persona owner) {
        QrCobroEntity qr = new QrCobroEntity();
        qr.setPropietaria(owner);
        qr.setRutaArchivo("qr-cobro/" + owner.getCodper() + "/00000000-0000-0000-0000-000000000001.png");
        qr.setNombreArchivo("qr.png");
        qr.setTipoContenido("image/png");
        qr.setFechaInicio(LocalDate.of(2026, 1, 1));
        qr.setFechaFin(LocalDate.of(2027, 12, 31));
        qr.setEstado(QrCobroEstado.ACTIVO);
        qr.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 18, 0));
        return qr;
    }

    private PagoEntity payment(Fixture fixture, QrCobroEntity qr, MetodoPago method) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(fixture.quota());
        pago.setQrCobro(qr);
        pago.setMonto(new BigDecimal("250.00"));
        pago.setMetodo(method);
        pago.setFechaPago(LocalDateTime.of(2026, 9, 25, 17, 0));
        pago.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 18, 0));
        pago.setEstado(PagoEstado.CONFIRMADO);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(fixture.user());
        pago.setRevisadoPor(fixture.user());
        pago.setIdempotencyKey(UUID.randomUUID());
        pago.setFechaRevision(LocalDateTime.of(2026, 9, 25, 18, 0));
        return pago;
    }

    private PagoComprobanteEntity proof(PagoEntity pago, String filename) {
        PagoComprobanteEntity comprobante = new PagoComprobanteEntity();
        comprobante.setPago(pago);
        comprobante.setRutaArchivo("comprobantes/" + pago.getCodpag() + "/" + filename);
        comprobante.setNombreArchivo("captura.png");
        comprobante.setTipoContenido("image/png");
        comprobante.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 18, 0));
        return comprobante;
    }

    private Fixture fixture(String suffix) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Persona owner = persona(suffix + "-O-" + token);
        Usuario user = new Usuario();
        user.setLogin(("persist." + token).substring(0, Math.min(30, ("persist." + token).length())));
        user.setPasswd("hash-no-expuesto");
        user.setEstado((short) 1);
        user.setPersona(owner);
        user.setFechaCreacion(LocalDateTime.of(2026, 9, 25, 18, 0));
        user = usuarioRepository.saveAndFlush(user);
        PropiedadEntity property = new PropiedadEntity();
        property.setNombre("Propiedad pagos " + token);
        property.setTipo("CASA");
        property.setDireccion("Calle de prueba");
        property.setCiudad("La Paz");
        property.setPropietaria(owner);
        property.setInversionInicial(BigDecimal.ZERO);
        property.setEstado((short) 1);
        property = propiedadRepository.saveAndFlush(property);
        UnidadEntity unit = new UnidadEntity();
        unit.setPropiedad(property);
        unit.setNombre("Unidad " + token);
        unit.setTipoUnidad("CASA");
        unit.setArea(new BigDecimal("40.00"));
        unit.setDormitorios((short) 1);
        unit.setBanos((short) 1);
        unit.setPiso(1);
        unit.setPrecioBase(new BigDecimal("2500.00"));
        unit.setEstadoOperativo((short) 1);
        unit = unidadRepository.saveAndFlush(unit);
        ContratoEntity contract = new ContratoEntity();
        contract.setUnidad(unit);
        contract.setInquilino(persona(suffix + "-T-" + token));
        contract.setFechaInicio(LocalDate.of(2026, 9, 1));
        contract.setFechaFin(LocalDate.of(2027, 9, 1));
        contract.setMontoMensual(new BigDecimal("2500.00"));
        contract.setMoneda("BOB");
        contract.setGarantia(BigDecimal.ZERO);
        contract.setEstado(ContratoEstado.VIGENTE);
        contract.setFechaRegistro(LocalDateTime.of(2026, 9, 25, 18, 0));
        contract = contratoRepository.saveAndFlush(contract);
        CuotaEntity quota = new CuotaEntity();
        quota.setContrato(contract);
        quota.setPeriodo(LocalDate.of(2026, 9, 1));
        quota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        quota.setMonto(new BigDecimal("2500.00"));
        quota.setEstado(CuotaEstado.PENDIENTE);
        quota = cuotaRepository.saveAndFlush(quota);
        return new Fixture(owner, user, quota);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci.substring(0, Math.min(20, ci.length())));
        persona.setNombre("Persona de pagos");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return personaRepository.saveAndFlush(persona);
    }

    private record Fixture(Persona owner, Usuario user, CuotaEntity quota) {
    }
}
