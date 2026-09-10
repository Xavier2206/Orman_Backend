package com.orman.backend.contract.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.BusinessRuleException;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.contract.dto.request.ContratoRenovacionRequest;
import com.orman.backend.contract.dto.request.ContratoRequest;
import com.orman.backend.contract.dto.request.RescisionContratoRequest;
import com.orman.backend.contract.dto.response.ContratoResponse;
import com.orman.backend.contract.entity.ContratoEntity;
import com.orman.backend.contract.entity.ContratoEstado;
import com.orman.backend.contract.mapper.ContratoMapper;
import com.orman.backend.contract.repository.ContratoRepository;
import com.orman.backend.contract.service.ContractOwnershipService;
import com.orman.backend.contract.service.ContratoService;
import com.orman.backend.contract.service.CuotaService;
import com.orman.backend.person.entity.Persona;
import com.orman.backend.person.repository.PersonaRepository;
import com.orman.backend.property.entity.UnidadEntity;
import com.orman.backend.property.service.PropertyOwnershipService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
public class ContratoServiceImpl implements ContratoService {

    private static final short ACTIVO = 1;

    private final ContratoRepository contratoRepository;
    private final PersonaRepository personaRepository;
    private final ContratoMapper contratoMapper;
    private final ContractOwnershipService contractOwnershipService;
    private final PropertyOwnershipService propertyOwnershipService;
    private final CuotaService cuotaService;

    @Override
    @Transactional
    public ContratoResponse createDraft(Integer coduni, ContratoRequest request, Authentication authentication) {
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidad(coduni, authentication);
        assertUnidadOperativa(unidad);
        validateDates(request.fechaInicio(), request.fechaFin());
        Persona inquilino = findActiveInquilino(request.codperInquilino());
        ContratoEntity saved = contratoRepository.saveAndFlush(contratoMapper.toEntity(request, unidad, inquilino));
        return contratoMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> listByUnidad(Integer coduni, Pageable pageable, Authentication authentication) {
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidad(coduni, authentication);
        Page<ContratoResponse> page = contratoRepository.findAllByUnidadOwned(coduni,
                unidad.getPropiedad().getPropietaria().getCodper(), defaultSort(pageable))
                .map(contratoMapper::toResponse);
        return pageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContratoResponse> list(Integer coduni, ContratoEstado estado, Pageable pageable,
                                                Authentication authentication) {
        Integer codper = propertyOwnershipService.currentPropietaria(authentication).getCodper();
        Page<ContratoResponse> page = contratoRepository.searchOwned(codper, coduni, estado, defaultSort(pageable))
                .map(contratoMapper::toResponse);
        return pageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse get(Integer codcon, Authentication authentication) {
        return contratoMapper.toResponse(contractOwnershipService.findOwnedContrato(codcon, authentication));
    }

    @Override
    @Transactional
    public ContratoResponse updateDraft(Integer codcon, ContratoRequest request, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.BORRADOR, "Solo se puede editar un Contrato en borrador.");
        validateDates(request.fechaInicio(), request.fechaFin());
        Persona inquilino = findActiveInquilino(request.codperInquilino());
        contratoMapper.update(contrato, request, inquilino);
        return contratoMapper.toResponse(contratoRepository.saveAndFlush(contrato));
    }

    @Override
    @Transactional
    public ContratoResponse confirm(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.BORRADOR, "Solo se puede confirmar un Contrato en borrador.");
        UnidadEntity unidad = contractOwnershipService.findOwnedUnidadForUpdate(contrato.getUnidad().getCoduni(), authentication);
        assertUnidadOperativa(unidad);
        if (contratoRepository.existsByUnidadCoduniAndEstado(unidad.getCoduni(), ContratoEstado.VIGENTE)) {
            throw new ConflictException("La Unidad ya tiene un Contrato vigente.");
        }
        contrato.setEstado(ContratoEstado.VIGENTE);
        contrato.setFechaConfirmacion(LocalDateTime.now(ZoneOffset.UTC));
        ContratoEntity saved = contratoRepository.saveAndFlush(contrato);
        cuotaService.generatePending(saved);
        return contratoMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContratoResponse finish(Integer codcon, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.VIGENTE, "Solo se puede finalizar un Contrato vigente.");
        contrato.setEstado(ContratoEstado.FINALIZADO);
        return contratoMapper.toResponse(contratoRepository.saveAndFlush(contrato));
    }

    @Override
    @Transactional
    public ContratoResponse renew(Integer codcon, ContratoRenovacionRequest request, Authentication authentication) {
        ContratoEntity origen = contractOwnershipService.findOwnedContrato(codcon, authentication);
        if (origen.getEstado() == ContratoEstado.BORRADOR) {
            throw new BusinessRuleException("Un Contrato en borrador no puede renovarse.");
        }
        validateDates(request.fechaInicio(), request.fechaFin());
        ContratoEntity saved = contratoRepository.saveAndFlush(contratoMapper.toRenewalEntity(request, origen));
        return contratoMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContratoResponse rescind(Integer codcon, RescisionContratoRequest request, Authentication authentication) {
        ContratoEntity contrato = contractOwnershipService.findOwnedContratoForUpdate(codcon, authentication);
        assertState(contrato, ContratoEstado.VIGENTE, "Solo se puede rescindir un Contrato vigente.");
        if (request.fechaRescision().isBefore(contrato.getFechaInicio())
                || !request.fechaRescision().isBefore(contrato.getFechaFin())) {
            throw new BusinessRuleException("La fecha de rescisión debe pertenecer al período contractual.");
        }
        contrato.setEstado(ContratoEstado.RESCINDIDO);
        contrato.setFechaRescision(request.fechaRescision());
        contrato.setMotivoRescision(request.motivoRescision().trim());
        return contratoMapper.toResponse(contratoRepository.saveAndFlush(contrato));
    }

    private Persona findActiveInquilino(Integer codperInquilino) {
        Persona inquilino = personaRepository.findById(codperInquilino)
                .orElseThrow(() -> new BusinessRuleException("La Persona inquilina no existe."));
        if (!Short.valueOf(ACTIVO).equals(inquilino.getEstado())) {
            throw new BusinessRuleException("La Persona inquilina debe estar activa.");
        }
        return inquilino;
    }

    private void assertUnidadOperativa(UnidadEntity unidad) {
        if (!Short.valueOf(ACTIVO).equals(unidad.getEstadoOperativo())) {
            throw new BusinessRuleException("La Unidad debe estar operativa para gestionar el Contrato.");
        }
    }

    private void validateDates(LocalDate fechaInicio, LocalDate fechaFin) {
        if (!fechaInicio.isBefore(fechaFin)) {
            throw new BusinessRuleException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        if (fechaInicio.getDayOfMonth() != 1 || fechaFin.getDayOfMonth() != 1) {
            throw new BusinessRuleException("Las fechas del Contrato deben ser el primer día del mes.");
        }
    }

    private void assertState(ContratoEntity contrato, ContratoEstado expected, String message) {
        if (contrato.getEstado() != expected) {
            throw new BusinessRuleException(message);
        }
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "fechaInicio"));
    }

    private PageResponse<ContratoResponse> pageResponse(Page<ContratoResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
