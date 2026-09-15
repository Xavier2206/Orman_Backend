package com.orman.backend.payment.entity;

import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.repository.CuentaPagoRepository;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.repository.ReciboRepository;
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
    @Autowired private CuentaPagoRepository cuentaPagoRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PagoComprobanteRepository comprobanteRepository;
    @Autowired private ReciboRepository reciboRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesPaymentActorsWithVersionSeventeen() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' ORDER BY table_name
                """, String.class);

        assertThat(tables).contains("cuentas_pago", "pagos", "pago_comprobantes", "recibos");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '17' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'pagos'::regclass", String.class))
                .contains("pk_pagos", "fk_pagos_cuotas", "fk_pagos_cuentas_pago", "uk_pagos_idempotency_key",
                        "ck_pagos_monto", "ck_pagos_metodo", "ck_pagos_estado", "ck_pagos_origen_registro",
                        "fk_pagos_usuarios_registrador", "fk_pagos_usuarios_revisor");
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'recibos'", String.class))
                .contains("uk_recibos_codpag");
    }

    @Test
    void persistsPaymentProofReceiptAndAccountRelationships() {
        Persona propietaria = personaRepository.saveAndFlush(persona("PY-OWNER-001"));
        CuentaPagoEntity cuenta = cuentaPagoRepository.saveAndFlush(cuenta(propietaria));
        CuotaEntity cuota = cuota(propietaria, "PY-TENANT-001");
        PagoEntity pago = pagoRepository.saveAndFlush(pago(cuota, cuenta));
        PagoComprobanteEntity comprobante = new PagoComprobanteEntity();
        comprobante.setPago(pago);
        comprobante.setUrl("https://example.test/comprobante.pdf");
        comprobante.setNombreArchivo("comprobante.pdf");
        comprobante.setTipoContenido("application/pdf");
        comprobante.setOrden(0);
        comprobante.setFechaRegistro(LocalDateTime.now());
        comprobanteRepository.saveAndFlush(comprobante);
        ReciboEntity recibo = new ReciboEntity();
        recibo.setPago(pago);
        recibo.setFechaEmision(LocalDateTime.now());
        reciboRepository.saveAndFlush(recibo);

        assertThat(reciboRepository.findByPagoCodpag(pago.getCodpag()).orElseThrow().getPago().getCuota().getCodcuo())
                .isEqualTo(cuota.getCodcuo());
        assertThat(comprobanteRepository.findAllByPagoCodpagOrderByOrdenAscIdAsc(pago.getCodpag())).hasSize(1);
    }

    @Test
    void rejectsSecondReceiptForTheSamePayment() {
        Persona propietaria = personaRepository.saveAndFlush(persona("PY-OWNER-002"));
        CuentaPagoEntity cuenta = cuentaPagoRepository.saveAndFlush(cuenta(propietaria));
        PagoEntity pago = pagoRepository.saveAndFlush(pago(cuota(propietaria, "PY-TENANT-002"), cuenta));
        ReciboEntity recibo = new ReciboEntity();
        recibo.setPago(pago);
        recibo.setFechaEmision(LocalDateTime.now());
        reciboRepository.saveAndFlush(recibo);

        ReciboEntity duplicate = new ReciboEntity();
        duplicate.setPago(pago);
        duplicate.setFechaEmision(LocalDateTime.now());
        assertThatThrownBy(() -> reciboRepository.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnAccountForAnEffectivePayment() {
        Persona propietaria = personaRepository.saveAndFlush(persona("PY-OWNER-003"));
        CuentaPagoEntity cuenta = cuentaPagoRepository.saveAndFlush(cuenta(propietaria));
        CuotaEntity cuota = cuota(propietaria, "PY-TENANT-003");
        String actor = usuarioRepository.findByPersonaCodper(propietaria.getCodper()).orElseThrow().getLogin();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO pagos (codcuo, codcta, monto, metodo, fecha_pago, fecha_registro, estado, origen_registro,
                    idempotency_key, registrado_por)
                VALUES (?, ?, 10, 'EFECTIVO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                    'PENDIENTE_REVISION', 'PROPIETARIA', ?, ?)
                """, cuota.getCodcuo(), cuenta.getCodcta(), UUID.randomUUID(), actor))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CuentaPagoEntity cuenta(Persona propietaria) {
        CuentaPagoEntity cuenta = new CuentaPagoEntity();
        cuenta.setPropietaria(propietaria);
        cuenta.setBanco("Banco de prueba");
        cuenta.setNumeroCuenta("123456");
        cuenta.setTitular("Titular de prueba");
        cuenta.setOrden(0);
        cuenta.setEstado((short) 1);
        return cuenta;
    }

    private PagoEntity pago(CuotaEntity cuota, CuentaPagoEntity cuenta) {
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setCuentaPago(cuenta);
        pago.setMonto(new BigDecimal("250.00"));
        pago.setMetodo(MetodoPago.TRANSFERENCIA);
        pago.setFechaPago(LocalDateTime.now());
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setEstado(PagoEstado.CONFIRMADO);
        Usuario actor = usuarioRepository.findByPersonaCodper(
                cuota.getContrato().getUnidad().getPropiedad().getPropietaria().getCodper()).orElseThrow();
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(actor);
        pago.setRevisadoPor(actor);
        pago.setIdempotencyKey(UUID.randomUUID());
        pago.setFechaRevision(LocalDateTime.now());
        return pago;
    }

    private CuotaEntity cuota(Persona propietaria, String tenantCi) {
        if (usuarioRepository.findByPersonaCodper(propietaria.getCodper()).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setLogin(("pay." + propietaria.getCodper()).substring(0,
                    Math.min(30, ("pay." + propietaria.getCodper()).length())));
            usuario.setPasswd("hash-no-expuesto");
            usuario.setEstado((short) 1);
            usuario.setPersona(propietaria);
            usuario.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.saveAndFlush(usuario);
        }
        PropiedadEntity propiedad = new PropiedadEntity();
        propiedad.setNombre("Propiedad de pagos");
        propiedad.setTipo("CASA");
        propiedad.setDireccion("Calle de prueba");
        propiedad.setCiudad("La Paz");
        propiedad.setPropietaria(propietaria);
        propiedad.setInversionInicial(BigDecimal.ZERO);
        propiedad.setEstado((short) 1);
        propiedad = propiedadRepository.saveAndFlush(propiedad);
        UnidadEntity unidad = new UnidadEntity();
        unidad.setPropiedad(propiedad);
        unidad.setNombre("Unidad de pagos");
        unidad.setTipoUnidad("DEPARTAMENTO");
        unidad.setArea(new BigDecimal("40.00"));
        unidad.setDormitorios((short) 1);
        unidad.setBanos((short) 1);
        unidad.setPiso(1);
        unidad.setPrecioBase(new BigDecimal("2500.00"));
        unidad.setEstadoOperativo((short) 1);
        unidad = unidadRepository.saveAndFlush(unidad);
        ContratoEntity contrato = new ContratoEntity();
        contrato.setUnidad(unidad);
        contrato.setInquilino(personaRepository.saveAndFlush(persona(tenantCi)));
        contrato.setFechaInicio(LocalDate.of(2026, 9, 1));
        contrato.setFechaFin(LocalDate.of(2027, 9, 1));
        contrato.setMontoMensual(new BigDecimal("2500.00"));
        contrato.setMoneda("BOB");
        contrato.setGarantia(BigDecimal.ZERO);
        contrato.setEstado(ContratoEstado.VIGENTE);
        contrato.setFechaRegistro(LocalDateTime.now());
        contrato = contratoRepository.saveAndFlush(contrato);
        CuotaEntity cuota = new CuotaEntity();
        cuota.setContrato(contrato);
        cuota.setPeriodo(LocalDate.of(2026, 9, 1));
        cuota.setFechaVencimiento(LocalDate.of(2026, 9, 1));
        cuota.setMonto(new BigDecimal("2500.00"));
        cuota.setEstado(CuotaEstado.PENDIENTE);
        return cuotaRepository.saveAndFlush(cuota);
    }

    private Persona persona(String ci) {
        Persona persona = new Persona();
        persona.setCi(ci);
        persona.setNombre("Persona de pagos");
        persona.setGenero('F');
        persona.setEstado((short) 1);
        persona.setCorreo(ci.toLowerCase() + "@example.test");
        persona.setTelefono("70000000");
        persona.setTipoPersona('A');
        return persona;
    }
}
