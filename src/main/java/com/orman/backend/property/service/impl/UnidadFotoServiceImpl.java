package com.orman.backend.property.service.impl;

import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.property.dto.request.UnidadFotoRequest;
import com.orman.backend.property.dto.response.UnidadFotoResponse;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.entity.UnidadFotoEntity;
import com.orman.backend.property.mapper.UnidadFotoMapper;
import com.orman.backend.property.repository.UnidadFotoRepository;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.UnidadFotoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnidadFotoServiceImpl implements UnidadFotoService {

    private final UnidadRepository unidadRepository;
    private final UnidadFotoRepository unidadFotoRepository;
    private final UnidadFotoMapper unidadFotoMapper;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional
    public UnidadFotoResponse create(Integer coduni, UnidadFotoRequest request, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidad(coduni, authentication);
        assertAvailableOrden(coduni, request.orden(), null);
        try {
            UnidadFotoEntity saved = unidadFotoRepository.saveAndFlush(unidadFotoMapper.toEntity(request, unidad));
            return unidadFotoMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadFotoResponse> listByUnidad(Integer coduni, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        return unidadFotoRepository
                .findAllByUnidadCoduniAndUnidadPropiedadPropietariaCodperOrderByOrdenAscIdAsc(coduni, codper)
                .stream().map(unidadFotoMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UnidadFotoResponse update(Integer coduni, Integer id, UnidadFotoRequest request, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        UnidadFotoEntity foto = findFoto(coduni, id);
        assertAvailableOrden(coduni, request.orden(), id);
        try {
            unidadFotoMapper.update(foto, request);
            return unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateOrden();
        }
    }

    @Override
    @Transactional
    public UnidadFotoResponse setPortada(Integer coduni, Integer id, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduniForUpdate(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        UnidadFotoEntity foto = findFoto(coduni, id);
        unidadFotoRepository.clearPortadaByUnidadCoduni(coduni);
        foto.setPortada(true);
        return unidadFotoMapper.toResponse(unidadFotoRepository.saveAndFlush(foto));
    }

    @Override
    @Transactional
    public void delete(Integer coduni, Integer id, Authentication authentication) {
        findOwnedUnidad(coduni, authentication);
        unidadFotoRepository.delete(findFoto(coduni, id));
    }

    private UnidadEntity findOwnedUnidad(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduni(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    private UnidadFotoEntity findFoto(Integer coduni, Integer id) {
        return unidadFotoRepository.findByIdAndUnidadCoduni(id, coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Fotografía de Unidad no encontrada."));
    }

    private void assertAvailableOrden(Integer coduni, Integer orden, Integer id) {
        boolean exists = id == null
                ? unidadFotoRepository.existsByUnidadCoduniAndOrden(coduni, orden)
                : unidadFotoRepository.existsByUnidadCoduniAndOrdenAndIdNot(coduni, orden, id);
        if (exists) {
            throw duplicateOrden();
        }
    }

    private ConflictException duplicateOrden() {
        return new ConflictException("El orden de la fotografía ya existe en esta Unidad.");
    }
}
