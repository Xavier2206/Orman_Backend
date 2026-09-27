package com.orman.backend.payment.service;

import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.entity.CuotaEstado;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.dto.request.PagoMotivoRequest;
import com.orman.backend.payment.dto.response.PagoResponse;
import com.orman.backend.payment.entity.MetodoPago;
import com.orman.backend.payment.entity.OrigenRegistroPago;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.mapper.PagoComprobanteMapper;
import com.orman.backend.payment.mapper.PagoMapper;
import com.orman.backend.payment.repository.PagoComprobanteRepository;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.impl.PagoServiceImpl;
import com.orman.backend.payment.service.PaymentImageStorageService;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.payment.service.QrCobroService;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceAnnulLockOrderTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private PagoComprobanteRepository pagoComprobanteRepository;
    @Mock private PagoMapper pagoMapper;
    @Mock private PagoComprobanteMapper pagoComprobanteMapper;
    @Mock private PaymentOwnershipService paymentOwnershipService;
    @Mock private PropertyOwnershipService propertyOwnershipService;
    @Mock private QrCobroService qrCobroService;
    @Mock private PaymentImageStorageService imageStorageService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private Clock clock;

    @InjectMocks private PagoServiceImpl pagoService;

    @Test
    void confirmedAnnulLocksQuotaBeforePaymentAndRecalculatesAfterward() {
        Integer codpag = 25;
        Integer codcuo = 40;
        Authentication authentication = new TestingAuthenticationToken(
                new AuthenticatedUser("owner.lock", UUID.randomUUID()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PROPIETARIO")));
        Usuario owner = new Usuario();
        owner.setLogin("owner.lock");
        ContratoEntity contrato = new ContratoEntity();
        contrato.setEstado(ContratoEstado.VIGENTE);
        CuotaEntity cuota = new CuotaEntity();
        ReflectionTestUtils.setField(cuota, "codcuo", codcuo);
        cuota.setContrato(contrato);
        cuota.setMonto(new BigDecimal("100.00"));
        cuota.setEstado(CuotaEstado.PARCIAL);
        PagoEntity pago = new PagoEntity();
        pago.setCuota(cuota);
        pago.setMonto(new BigDecimal("40.00"));
        pago.setMetodo(MetodoPago.EFECTIVO);
        pago.setOrigenRegistro(OrigenRegistroPago.PROPIETARIA);
        pago.setEstado(com.orman.backend.payment.entity.PagoEstado.CONFIRMADO);
        PagoResponse response = new PagoResponse(codpag, codcuo, null, pago.getMonto(), "EFECTIVO",
                null, null, "ANULADO", "PROPIETARIA", owner.getLogin(), owner.getLogin(), null,
                null, "Corrección");

        when(paymentOwnershipService.findOwnedPago(codpag, authentication)).thenReturn(pago);
        when(paymentOwnershipService.findOwnedCuotaForUpdate(codcuo, authentication)).thenReturn(cuota);
        when(paymentOwnershipService.findOwnedPagoForUpdate(codpag, authentication)).thenReturn(pago);
        when(usuarioRepository.findByLoginWithPersona("owner.lock")).thenReturn(Optional.of(owner));
        when(clock.instant()).thenReturn(Instant.parse("2026-09-25T18:00:00Z"));
        when(pagoRepository.sumConfirmedMontoByCuota(codcuo)).thenReturn(BigDecimal.ZERO);
        when(pagoMapper.toResponse(pago)).thenReturn(response);

        pagoService.annul(codpag, new PagoMotivoRequest("Corrección"), authentication);

        InOrder lockOrder = inOrder(paymentOwnershipService);
        lockOrder.verify(paymentOwnershipService).findOwnedPago(codpag, authentication);
        lockOrder.verify(paymentOwnershipService).findOwnedCuotaForUpdate(codcuo, authentication);
        lockOrder.verify(paymentOwnershipService).findOwnedPagoForUpdate(codpag, authentication);
        InOrder recalculationOrder = inOrder(pagoRepository, cuotaRepository);
        recalculationOrder.verify(pagoRepository).saveAndFlush(pago);
        recalculationOrder.verify(pagoRepository).sumConfirmedMontoByCuota(codcuo);
        recalculationOrder.verify(cuotaRepository).saveAndFlush(cuota);
        verify(pagoMapper).toResponse(pago);
    }
}
