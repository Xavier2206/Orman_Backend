package com.orman.backend.process.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.process.dto.request.CreateProcesoRequest;
import com.orman.backend.process.dto.request.UpdateProcesoRequest;
import com.orman.backend.process.dto.response.ProcesoResponse;
import com.orman.backend.process.entity.Proceso;
import com.orman.backend.process.mapper.ProcesoMapper;
import com.orman.backend.process.repository.ProcesoRepository;
import com.orman.backend.process.service.ProcesoService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcesoServiceImpl implements ProcesoService {
    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;
    private final ProcesoRepository procesoRepository;
    private final ProcesoMapper procesoMapper;
    private final EntityManager entityManager;
    @Override @Transactional public ProcesoResponse create(CreateProcesoRequest request) {
        String nombre = procesoMapper.normalizeNombre(request.nombre()); String enlace = procesoMapper.normalizeEnlace(request.enlace());
        if (procesoRepository.existsByNombre(nombre)) throw duplicateNombre();
        if (procesoRepository.existsByEnlace(enlace)) throw duplicateEnlace();
        try { Proceso saved = procesoRepository.saveAndFlush(procesoMapper.toEntity(request)); entityManager.refresh(saved); return procesoMapper.toResponse(saved); }
        catch (DataIntegrityViolationException exception) { throw translateDuplicate(nombre, enlace); }
    }
    @Override @Transactional(readOnly = true) public ProcesoResponse get(Integer codp) { return procesoMapper.toResponse(findProceso(codp)); }
    @Override @Transactional(readOnly = true) public PageResponse<ProcesoResponse> list(Pageable pageable) {
        Page<ProcesoResponse> page = procesoRepository.findAllByOrderByNombreAsc(pageable).map(procesoMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
    @Override @Transactional public ProcesoResponse update(Integer codp, UpdateProcesoRequest request) {
        Proceso proceso = findProceso(codp); String nombre = procesoMapper.normalizeNombre(request.nombre()); String enlace = procesoMapper.normalizeEnlace(request.enlace());
        if (procesoRepository.existsByNombreAndCodpNot(nombre, codp)) throw duplicateNombre();
        if (procesoRepository.existsByEnlaceAndCodpNot(enlace, codp)) throw duplicateEnlace();
        try { procesoMapper.update(proceso, request); return procesoMapper.toResponse(procesoRepository.saveAndFlush(proceso)); }
        catch (DataIntegrityViolationException exception) { throw translateDuplicate(nombre, enlace); }
    }
    @Override @Transactional public ProcesoResponse activate(Integer codp) { return changeStatus(codp, ACTIVO); }
    @Override @Transactional public ProcesoResponse deactivate(Integer codp) { return changeStatus(codp, INACTIVO); }
    private ProcesoResponse changeStatus(Integer codp, short estado) { Proceso proceso = findProceso(codp); proceso.setEstado(estado); return procesoMapper.toResponse(procesoRepository.save(proceso)); }
    private Proceso findProceso(Integer codp) { return procesoRepository.findById(codp).orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado.")); }
    private ConflictException duplicateNombre() { return new ConflictException("El nombre del Proceso ya está registrado."); }
    private ConflictException duplicateEnlace() { return new ConflictException("El enlace del Proceso ya está registrado."); }
    private ConflictException translateDuplicate(String nombre, String enlace) { return procesoRepository.existsByNombre(nombre) ? duplicateNombre() : duplicateEnlace(); }
}
