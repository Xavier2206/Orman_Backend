package com.orman.backend.property.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.property.dto.request.PropiedadRequest;
import com.orman.backend.property.dto.response.PropiedadResponse;
import com.orman.backend.property.dto.response.PropiedadResumenResponse;
import com.orman.backend.property.entity.PropiedadEntity;
import com.orman.backend.property.mapper.PropiedadMapper;
import com.orman.backend.property.repository.PropiedadRepository;
import com.orman.backend.property.repository.PropiedadResumenProjection;
import com.orman.backend.property.repository.PropiedadUnidadCountProjection;
import com.orman.backend.property.repository.UnidadRepository;
import com.orman.backend.property.service.PropertyOwnershipService;
import com.orman.backend.property.service.PropiedadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final UnidadRepository unidadRepository;
    private final PropiedadMapper propiedadMapper;
    private final PropertyOwnershipService propertyOwnershipService;

    @Override
    @Transactional(readOnly = true)
    public PropiedadResumenResponse resumen(Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        PropiedadResumenProjection resumen = propiedadRepository.findResumenByPropietaria(codper);
        BigDecimal ocupacionGlobal = resumen.getUnidadesHabilitadas() == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(resumen.getUnidadesOcupadas())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(resumen.getUnidadesHabilitadas()), 2, RoundingMode.HALF_UP);
        return new PropiedadResumenResponse(resumen.getInversionTotal(), resumen.getPropiedadesActivas(),
                resumen.getCasasActivas(), resumen.getEdificiosActivos(), resumen.getUnidadesTotales(),
                resumen.getUnidadesHabilitadas(), resumen.getUnidadesNoHabilitadas(), resumen.getUnidadesOcupadas(),
                ocupacionGlobal);
    }

    @Override
    @Transactional
    public PropiedadResponse create(PropiedadRequest request, Authentication authentication) {
        Persona propietaria = propertyOwnershipService.currentPropietaria(authentication);
        propertyOwnershipService.assertCurrentPropietaria(authentication, request.codperPropietaria());
        validateCoordinates(request);
        PropiedadEntity saved = propiedadRepository.save(propiedadMapper.toEntity(request, propietaria));
        return propiedadMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropiedadResponse> list(String q, String tipo, Short estado, Pageable pageable,
                                                 Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<PropiedadEntity> propiedades = propiedadRepository.searchOwned(codper, normalizeQuery(q),
                normalizeTipo(tipo), estado, defaultSort(pageable));
        Map<Integer, PropiedadUnidadCountProjection> unidadesPorPropiedad = countUnitsByProperty(propiedades, codper);
        Page<PropiedadResponse> page = propiedades.map(propiedad -> {
            PropiedadUnidadCountProjection metricas = unidadesPorPropiedad.get(propiedad.getCodprop());
            long cantidadUnidades = metricas == null ? 0 : metricas.getCantidadUnidades();
            long unidadesHabilitadas = metricas == null ? 0 : metricas.getUnidadesHabilitadas();
            long unidadesOcupadas = metricas == null ? 0 : metricas.getUnidadesOcupadas();
            return propiedadMapper.toResponse(propiedad, cantidadUnidades, unidadesHabilitadas, unidadesOcupadas,
                    calculateOccupancy(unidadesHabilitadas, unidadesOcupadas));
        });
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PropiedadResponse get(Integer codprop, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        return propiedadMapper.toResponse(propiedad, countUnits(propiedad));
    }

    @Override
    @Transactional
    public PropiedadResponse update(Integer codprop, PropiedadRequest request, Authentication authentication) {
        PropiedadEntity propiedad = findOwned(codprop, authentication);
        propertyOwnershipService.assertCurrentPropietaria(authentication, request.codperPropietaria());
        validateCoordinates(request);
        propiedadMapper.update(propiedad, request, propiedad.getPropietaria());
        PropiedadEntity saved = propiedadRepository.save(propiedad);
        return propiedadMapper.toResponse(saved, countUnits(saved));
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
        PropiedadEntity saved = propiedadRepository.save(propiedad);
        return propiedadMapper.toResponse(saved, countUnits(saved));
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

    private Map<Integer, PropiedadUnidadCountProjection> countUnitsByProperty(Page<PropiedadEntity> propiedades,
                                                                                Integer codper) {
        if (propiedades.isEmpty()) {
            return Map.of();
        }
        return unidadRepository.countByPropiedadesOwned(
                        propiedades.getContent().stream().map(PropiedadEntity::getCodprop).toList(), codper)
                .stream()
                .collect(Collectors.toMap(PropiedadUnidadCountProjection::getCodprop, metricas -> metricas));
    }

    private BigDecimal calculateOccupancy(long unidadesHabilitadas, long unidadesOcupadas) {
        return unidadesHabilitadas == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(unidadesOcupadas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(unidadesHabilitadas), 2, RoundingMode.HALF_UP);
    }

    private long countUnits(PropiedadEntity propiedad) {
        return unidadRepository.countByPropiedadCodpropAndPropiedadPropietariaCodper(
                propiedad.getCodprop(), propiedad.getPropietaria().getCodper());
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
