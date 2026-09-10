package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.mapper.PropiedadMapper;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.PropiedadService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropiedadServiceImpl implements PropiedadService {

    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;

    private final PropiedadRepository propiedadRepository;
    private final PropiedadMapper propiedadMapper;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional
    public PropiedadResponse create(PropiedadRequest request, Authentication authentication) {
        Persona propietaria = propertyOwnershipService.currentPropietaria(authentication);
        propertyOwnershipService.assertCurrentPropietaria(authentication, request.codperPropietaria());
        validateCoordinates(request);
        PropiedadEntity saved = propiedadRepository.save(propiedadMapper.toEntity(request, propietaria));
        return propiedadMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropiedadResponse> list(String q, String tipo, Short estado, Pageable pageable,
                                                 Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<PropiedadResponse> page = propiedadRepository.searchOwned(codper, normalizeQuery(q), normalizeTipo(tipo),
                estado, defaultSort(pageable)).map(propiedadMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PropiedadResponse get(Integer codprop, Authentication authentication) {
        return propiedadMapper.toResponse(findOwned(codprop, authentication));
    }

    @Override
    @Transactional
    public PropiedadResponse update(Integer codprop, PropiedadRequest request, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        propertyOwnershipService.assertCurrentPropietaria(authentication, request.codperPropietaria());
        validateCoordinates(request);
        propiedadMapper.update(propiedad, request, propiedad.getPropietaria());
        return propiedadMapper.toResponse(propiedadRepository.save(propiedad));
    }

    @Override
    @Transactional
    public PropiedadResponse activate(Integer codprop, Authentication authentication) {
        return changeStatus(codprop, ACTIVO, authentication);
    }

    @Override
    @Transactional
    public PropiedadResponse deactivate(Integer codprop, Authentication authentication) {
        return changeStatus(codprop, INACTIVO, authentication);
    }

    private PropiedadResponse changeStatus(Integer codprop, short estado, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        propiedad.setEstado(estado);
        return propiedadMapper.toResponse(propiedadRepository.save(propiedad));
    }

    private PropiedadEntity findOwned(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = propiedadRepository.findById(codprop)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, propiedad.getPropietaria());
        return propiedad;
    }

    private void validateCoordinates(PropiedadRequest request) {
        if ((request.latitud() == null) != (request.longitud() == null)) {
            throw new BusinessRuleException("La latitud y la longitud deben registrarse juntas.");
        }
    }

    private String normalizeQuery(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String normalizeTipo(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "nombre"));
    }
}
