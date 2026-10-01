package com.orman.backend.payment.service;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.entity.PagoEstado;
import com.orman.backend.payment.mapper.PagoComprobanteMapper;
import com.orman.backend.payment.mapper.PagoMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.impl.PagoServiceImpl;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.payment.service.QrCobroService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceBusinessTimeTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private com.orman.backend.contract.repository.CuotaRepository cuotaRepository;
    @Mock private PagoComprobanteRepository comprobanteRepository;
    @Mock private PaymentOwnershipService ownershipService;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private QrCobroService qrCobroService;
    @Mock private PaymentImageStorageService imageStorageService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UsuarioRepository usuarioRepository;

    @Test
    void confirmsPaymentAtBoliviaWallTimeAndReturnsOffset() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T16:25:00Z"), ZoneOffset.UTC);
        PagoServiceImpl service = new PagoServiceImpl(pagoRepository, cuotaRepository, comprobanteRepository,
                new PagoMapper(), new PagoComprobanteMapper(), ownershipService, propertyOwnershipService,
                qrCobroService, imageStorageService, eventPublisher, usuarioRepository, clock);

        Usuario owner = new Usuario();
        owner.setLogin("owner.time");
        CuotaEntity cuota = new CuotaEntity();
        ReflectionTestUtils.setField(cuota, "codcuo", 7);
        cuota.setMonto(new BigDecimal("500.00"));
        cuota.setEstado(CuotaEstado.PENDIENTE);
        PagoEntity pago = new PagoEntity();
        ReflectionTestUtils.setField(pago, "codpag", 11);
        pago.setCuota(cuota);
        pago.setMonto(new BigDecimal("500.00"));
        pago.setMetodo(MetodoPago.QR);
        pago.setFechaPago(LocalDateTime.of(2026, 9, 30, 12, 20));
        pago.setFechaRegistro(LocalDateTime.of(2026, 9, 30, 12, 20));
        pago.setEstado(PagoEstado.PENDIENTE_REVISION);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setRegistradoPor(owner);
        pago.setIdempotencyKey(UUID.randomUUID());
        Authentication authentication = new TestingAuthenticationToken(
                new AuthenticatedUser("owner.time", UUID.randomUUID()), null);
        when(ownershipService.findOwnedPago(11, authentication)).thenReturn(pago);
        when(ownershipService.findOwnedCuotaForUpdate(7, authentication)).thenReturn(cuota);
        when(ownershipService.findOwnedPagoForUpdate(11, authentication)).thenReturn(pago);
        when(usuarioRepository.findByLoginWithPersona("owner.time")).thenReturn(Optional.of(owner));
        when(pagoRepository.sumConfirmedMontoByCuota(7)).thenReturn(BigDecimal.ZERO);

        var response = service.confirm(11, authentication);

        LocalDateTime expectedStored = LocalDateTime.of(2026, 9, 30, 12, 25);
        assertThat(pago.getFechaRevision()).isEqualTo(expectedStored);
        assertThat(response.fechaRevision()).isEqualTo(OffsetDateTime.of(
                2026, 9, 30, 12, 25, 0, 0, ZoneOffset.ofHours(-4)));
    }
}
