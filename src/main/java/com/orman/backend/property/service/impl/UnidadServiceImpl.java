package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.repository.ContratoRepository;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UnidadServiceImpl implements UnidadService {

    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;
    private static final List<ContratoEstado> ESTADOS_CONTRATO_BLOQUEANTES =
            List.of(ContratoEstado.PROGRAMADO, ContratoEstado.VIGENTE);

    private final PropiedadRepository propiedadRepository;
    private final UnidadRepository unidadRepository;
    private final ContratoRepository contratoRepository;
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
            return unidadMapper.toResponse(saved, true);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnidadResponse> listByPropiedad(Integer codprop, Short estadoOperativo, Pageable pageable,
                                                         Authentication authentication) {
        PropiedadEntity propiedad = findOwnedPropiedad(codprop, authentication);
        Integer codper = propiedad.getPropietaria().getCodper();
        Page<UnidadEntity> unitsPage = unidadRepository.findAllByPropiedadOwned(codprop, codper, estadoOperativo,
                defaultSort(pageable));
        Set<Integer> blockedUnitIds = blockingUnitIds(codper, unitsPage.getContent());
        Page<UnidadResponse> page = unitsPage.map(unidad -> unidadMapper.toResponse(unidad,
                !blockedUnitIds.contains(unidad.getCoduni())));
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadResponse get(Integer coduni, Authentication authentication) {
        return toResponse(findOwnedUnidad(coduni, authentication));
    }

    @Override
    @Transactional
    public UnidadResponse update(Integer coduni, UnidadRequest request, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidad(coduni, authentication);
        if (!Objects.equals(request.estadoOperativo(), unidad.getEstadoOperativo())) {
            throw new BusinessRuleException(
                    "El estado operativo de la unidad debe modificarse mediante las acciones de activar o desactivar.");
        }
        Integer codprop = unidad.getPropiedad().getCodprop();
        if (unidadRepository.existsByPropiedadCodpropAndNombreAndCoduniNot(codprop, request.nombre().trim(), coduni)) {
            throw duplicateNombre();
        }
        try {
            unidadMapper.update(unidad, request);
            return toResponse(unidadRepository.saveAndFlush(unidad));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    @Override
    @Transactional
    public UnidadResponse activate(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidadForUpdate(coduni, authentication);
        if (Objects.equals(unidad.getEstadoOperativo(), ACTIVO)) {
            return toResponse(unidad);
        }
        unidad.setEstadoOperativo(ACTIVO);
        return toResponse(unidadRepository.saveAndFlush(unidad));
    }

    @Override
    @Transactional
    public UnidadResponse deactivate(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = findOwnedUnidadForUpdate(coduni, authentication);
        if (Objects.equals(unidad.getEstadoOperativo(), INACTIVO)) {
            return toResponse(unidad);
        }
        if (hasBlockingContract(coduni)) {
            throw new BusinessRuleException(
                    "No se puede desactivar la unidad porque tiene un Contrato PROGRAMADO o VIGENTE.");
        }
        unidad.setEstadoOperativo(INACTIVO);
        return unidadMapper.toResponse(unidadRepository.saveAndFlush(unidad), true);
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

    private UnidadEntity findOwnedUnidadForUpdate(Integer coduni, Authentication authentication) {
        UnidadEntity unidad = unidadRepository.findByCoduniForUpdate(coduni)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada."));
        propertyOwnershipService.assertCurrentPropietaria(authentication, unidad.getPropiedad().getPropietaria());
        return unidad;
    }

    private UnidadResponse toResponse(UnidadEntity unidad) {
        return unidadMapper.toResponse(unidad, !hasBlockingContract(unidad.getCoduni()));
    }

    private boolean hasBlockingContract(Integer coduni) {
        return contratoRepository.existsByUnidadCoduniAndEstadoIn(coduni, ESTADOS_CONTRATO_BLOQUEANTES);
    }

    private Set<Integer> blockingUnitIds(Integer codper, Collection<UnidadEntity> units) {
        if (units.isEmpty()) {
            return Set.of();
        }
        List<Integer> codunis = units.stream().map(UnidadEntity::getCoduni).toList();
        return contratoRepository.findOwnedUnitIdsWithBlockingContracts(codper, codunis);
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
