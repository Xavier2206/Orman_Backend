package com.orman.backend.contract.service.impl;

import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractOwnershipServiceImpl implements ContractOwnershipService {

    private final UnidadRepository unidadRepository;
    private final ContratoRepository contratoRepository;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    public UnidadEntity findOwnedUnidad(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduni(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    @Override
    @Transactional
    public UnidadEntity findOwnedUnidadForUpdate(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduniForUpdate(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    @Override
    public ContratoEntity findOwnedContrato(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contratoRepository.findByCodcon(codcon)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado."));
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                contrato.getUnidad().getPropiedad().getPropietaria());
        return contrato;
    }

    @Override
    @Transactional
    public ContratoEntity findOwnedContratoForUpdate(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contratoRepository.findByCodconForUpdate(codcon)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato no encontrado."));
        propertyOwnershipService.assertCurrentPropietaria(authentication,
                contrato.getUnidad().getPropiedad().getPropietaria());
        return contrato;
    }
}
