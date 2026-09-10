package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.property.dto.request.UnidadRequest;
import com.orman.backend.property.dto.response.UnidadResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.mapper.UnidadMapper;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.UnidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnidadServiceImpl implements UnidadService {

    private final PropiedadRepository propiedadRepository;
    private final UnidadRepository unidadRepository;
    private final UnidadMapper unidadMapper;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional
    public UnidadResponse create(Integer codprop, UnidadRequest request, Authentication authentication) {
        PropiedadEntity propiedad = findOwnedPropiedad(codprop, authentication);
        if (unidadRepository.existsByPropiedadCodpropAndNombre(codprop, request.nombre().trim())) {
            throw duplicateNombre();
        }
        try {
            UnidadEntity saved = unidadRepository.saveAndFlush(unidadMapper.toEntity(request, propiedad));
            return unidadMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnidadResponse> listByPropiedad(Integer codprop, Pageable pageable,
                                                         Authentication authentication) {
        PropiedadEntity propiedad = findOwnedPropiedad(codprop, authentication);
        Page<UnidadResponse> page = unidadRepository.findAllByPropiedadOwned(codprop,
                propiedad.getPropietaria().getCodper(), defaultSort(pageable)).map(unidadMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadResponse get(Integer coduni, Authentication authentication) {
        return unidadMapper.toResponse(findOwnedUnidad(coduni, authentication));
    }

    @Override
    @Transactional
    public UnidadResponse update(Integer coduni, UnidadRequest request, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidad(coduni, authentication);
        Integer codprop = unidad.getPropiedad().getCodprop();
        if (unidadRepository.existsByPropiedadCodpropAndNombreAndCoduniNot(codprop, request.nombre().trim(), coduni)) {
            throw duplicateNombre();
        }
        try {
            unidadMapper.update(unidad, request);
            return unidadMapper.toResponse(unidadRepository.saveAndFlush(unidad));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    private PropiedadEntity findOwnedPropiedad(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = propiedadRepository.findById(codprop)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, propiedad.getPropietaria());
        return propiedad;
    }

    private UnidadEntity findOwnedUnidad(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduni(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "nombre"));
    }

    private ConflictException duplicateNombre() {
        return new ConflictException("El nombre de la Unidad ya existe en esta Propiedad.");
    }
}
