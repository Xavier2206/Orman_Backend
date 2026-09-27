package com.orman.backend.payment.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.auth.model.AuthenticatedUser;
import com.orman.backend.contract.entity.CuotaEntity;
import com.orman.backend.contract.repository.CuotaRepository;
import com.orman.backend.payment.entity.PagoEntity;
import com.orman.backend.payment.repository.PagoRepository;
import com.orman.backend.payment.service.PaymentOwnershipService;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.user.entity.Usuario;
import com.orman.backend.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentOwnershipServiceImpl implements PaymentOwnershipService {

    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final PropertyOwnershipService propertyOwnershipService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public CuotaEntity findOwnedCuota(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = cuotaRepository.findByCodcuo(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
        assertOwner(authentication, cuota);
        return cuota;
    }

    @Override
    public CuotaEntity findCuota(Integer codcuo) {
        return cuotaRepository.findByCodcuo(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
    }

    @Override
    public CuotaEntity findAccessibleCuota(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = findCuota(codcuo);
        assertParticipant(authentication, cuota);
        return cuota;
    }

    @Override
    @Transactional
    public CuotaEntity findOwnedCuotaForUpdate(Integer codcuo, Authentication authentication) {
        CuotaEntity cuota = cuotaRepository.findByCodcuoForUpdate(codcuo).orElseThrow(() -> cuotaNotFound(codcuo));
        assertOwner(authentication, cuota);
        return cuota;
    }

    @Override
    public PagoEntity findOwnedPago(Integer codpag, Authentication authentication) {
        PagoEntity pago = pagoRepository.findByCodpag(codpag).orElseThrow(() -> pagoNotFound(codpag));
        assertOwner(authentication, pago.getCuota());
        return pago;
    }

    @Override
    @Transactional
    public PagoEntity findOwnedPagoForUpdate(Integer codpag, Authentication authentication) {
        PagoEntity pago = pagoRepository.findByCodpagForUpdate(codpag).orElseThrow(() -> pagoNotFound(codpag));
        assertOwner(authentication, pago.getCuota());
        return pago;
    }

    @Override
    public PagoEntity findAccessiblePago(Integer codpag, Authentication authentication) {
        PagoEntity pago = pagoRepository.findByCodpag(codpag).orElseThrow(() -> pagoNotFound(codpag));
        assertParticipant(authentication, pago.getCuota());
        return pago;
    }

    private void assertOwner(Authentication authentication, CuotaEntity cuota) {
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                cuota.getContrato().getUnidad().getPropiedad().getPropietaria());
    }

    private void assertParticipant(Authentication authentication, CuotaEntity cuota) {
        if (hasRole(authentication, "ROLE_PROPIETARIO")) {
            assertOwner(authentication, cuota);
            return;
        }
        if (hasRole(authentication, "ROLE_INQUILINO")) {
            Usuario actor = currentUser(authentication);
            if (!actor.getPersona().getCodper().equals(cuota.getContrato().getInquilino().getCodper())) {
                throw new AccessDeniedException("La Cuota no pertenece al Inquilino autenticado.");
            }
            return;
        }
        throw new AccessDeniedException("No tiene autorización para consultar este recurso.");
    }

    private Usuario currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("No existe un Usuario autenticado válido.");
        }
        return usuarioRepository.findByLoginWithPersona(user.login())
                .orElseThrow(() -> new AccessDeniedException("El Usuario autenticado no existe."));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private ResourceNotFoundException cuotaNotFound(Integer codcuo) {
        return new ResourceNotFoundException("La Cuota solicitada no existe: " + codcuo);
    }

    private ResourceNotFoundException pagoNotFound(Integer codpag) {
        return new ResourceNotFoundException("El Pago solicitado no existe: " + codpag);
    }
}
