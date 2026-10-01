package com.orman.backend.role.service.impl;

import com.orman.backend.common.dto.PageResponse;
import com.orman.backend.common.exception.ResourceNotFoundException;
import com.orman.backend.role.dto.response.RolResumenResponse;
import com.orman.backend.role.dto.response.RolResponse;
import com.orman.backend.role.entity.Rol;
import com.orman.backend.role.mapper.RolMapper;
import com.orman.backend.role.repository.RolRepository;
import com.orman.backend.role.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {

    private static final short ACTIVO = 1;
    private static final short INACTIVO = 0;

    private final RolRepository rolRepository;
    private final RolMapper rolMapper;

    @Override
    @Transactional(readOnly = true)
    public RolResponse get(Integer codr) {
        return rolMapper.toResponse(findRol(codr));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RolResponse> list(Pageable pageable) {
        return list(null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RolResponse> list(String q, Short estado, Pageable pageable) {
        Page<RolResponse> page = rolRepository.search(normalizeQuery(q), estado, defaultSort(pageable))
                .map(rolMapper::toResponse);
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public RolResumenResponse resumen() {
        long activos = rolRepository.countByEstado(ACTIVO);
        long inactivos = rolRepository.countByEstado(INACTIVO);
        return new RolResumenResponse(rolRepository.count(), activos, inactivos);
    }

    private Rol findRol(Integer codr) {
        return rolRepository.findById(codr)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado."));
    }

    private String normalizeQuery(String q) {
        return q == null || q.trim().isEmpty() ? null : q.trim();
    }

    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "nombre"));
    }
}
