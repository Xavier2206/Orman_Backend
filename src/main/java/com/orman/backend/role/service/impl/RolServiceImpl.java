package com.orman.backend.role.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ConflictException;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.request.CreateRolRequest;
import com.orman.backend.role.dto.request.UpdateRolRequest;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.mapper.RolMapper;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.service.RolService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {

    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public RolResponse create(CreateRolRequest request) {
        String nombre = rolMapper.normalizeNombre(request.nombre());
        if (rolRepository.existsByNombre(nombre)) {
            throw duplicateNombre();
        }
        try {
            Rol saved = rolRepository.saveAndFlush(rolMapper.toEntity(request));
            entityManager.refresh(saved);
            return rolMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponse get(Integer codr) {
        return rolMapper.toResponse(findRol(codr));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RolResponse> list(Pageable pageable) {
        Page<RolResponse> page = rolRepository.findAllByOrderByNombreAsc(pageable).map(rolMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional
    public RolResponse update(Integer codr, UpdateRolRequest request) {
        Rol rol = findRol(codr);
        String nombre = rolMapper.normalizeNombre(request.nombre());
        if (rolRepository.existsByNombreAndCodrNot(nombre, codr)) {
            throw duplicateNombre();
        }
        try {
            rolMapper.update(rol, request);
            return rolMapper.toResponse(rolRepository.saveAndFlush(rol));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateNombre();
        }
    }

    @Override
    @Transactional
    public RolResponse activate(Integer codr) {
        return changeStatus(codr, ACTIVO);
    }

    @Override
    @Transactional
    public RolResponse deactivate(Integer codr) {
        return changeStatus(codr, INACTIVO);
    }

    private RolResponse changeStatus(Integer codr, short estado) {
        Rol rol = findRol(codr);
        rol.setEstado(estado);
        return rolMapper.toResponse(rolRepository.save(rol));
    }

    private Rol findRol(Integer codr) {
        return rolRepository.findById(codr)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
    }

    private ConflictException duplicateNombre() {
        return new ConflictException("El nombre del Rol ya estÃ¡ registrado.");
    }
}
